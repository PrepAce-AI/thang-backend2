package backend.service;

import backend.dto.request.PurchaseRequest;
import backend.dto.response.PaymentResponse;
import backend.entity.Course;
import backend.entity.Enrollment;
import backend.entity.Payment;
import backend.exceptions.BadRequestException;
import backend.exceptions.ResourceNotFoundException;
import backend.repository.CourseRepository;
import backend.repository.EnrollmentRepository;
import backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * UC-14: Purchase Packages
 * Xử lý giao dịch mua khóa học. Sau khi thanh toán thành công → tự động enroll.
 * Kiến trúc hiện tại: mock/simulate gateway, dễ tích hợp VNPAY/MOMO sau.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    // ─── Mua khóa học ───────────────────────────────────────────────────────────

    @Transactional
    public PaymentResponse purchaseCourse(Integer studentId, PurchaseRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course không tồn tại: " + request.getCourseId()));

        if (!Boolean.TRUE.equals(course.getIsPublished())) {
            throw new BadRequestException("Khóa học này chưa được phát hành");
        }

        // Kiểm tra đã mua chưa
        if (paymentRepository.existsSuccessfulPayment(studentId, request.getCourseId())) {
            throw new BadRequestException("Bạn đã sở hữu khóa học này rồi");
        }

        // Kiểm tra đã enroll chưa (trường hợp free)
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, request.getCourseId())) {
            throw new BadRequestException("Bạn đã được đăng ký vào khóa học này");
        }

        String transactionCode = request.getTransactionCode() != null
                ? request.getTransactionCode()
                : "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Xác định trạng thái thanh toán
        String status = processPaymentGateway(request.getPaymentMethod(), transactionCode, course);

        // Lưu payment record
        Payment payment = new Payment();
        payment.setStudentId(studentId);
        payment.setCourseId(course.getId());
        payment.setAmount(course.getPrice());
        payment.setPaymentMethod(request.getPaymentMethod().toUpperCase());
        payment.setPaymentStatus(status);
        payment.setTransactionCode(transactionCode);
        payment.setPaidAt(new Date());
        Payment saved = paymentRepository.save(payment);

        // Nếu SUCCESS → tự động enroll
        if ("SUCCESS".equals(status)) {
            autoEnroll(studentId, course.getId());
            log.info("Student {} purchased courseId={} via {} — enrolled", studentId, course.getId(), request.getPaymentMethod());
        }

        return PaymentResponse.builder()
                .paymentId(saved.getPaymentId())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .amount(saved.getAmount())
                .paymentMethod(saved.getPaymentMethod())
                .paymentStatus(saved.getPaymentStatus())
                .transactionCode(saved.getTransactionCode())
                .paidAt(saved.getPaidAt())
                .message("SUCCESS".equals(status) ? "Thanh toán thành công! Bạn đã được đăng ký vào khóa học." : "Thanh toán đang xử lý.")
                .build();
    }

    /**
     * Callback từ cổng VNPAY/MOMO xác nhận giao dịch thành công.
     * Sẽ được gọi từ webhook endpoint.
     */
    @Transactional
    public PaymentResponse confirmPayment(String transactionCode) {
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Giao dịch không tồn tại: " + transactionCode));

        if ("SUCCESS".equals(payment.getPaymentStatus())) {
            throw new BadRequestException("Giao dịch này đã được xác nhận trước đó");
        }

        payment.setPaymentStatus("SUCCESS");
        payment.setPaidAt(new Date());
        paymentRepository.save(payment);

        autoEnroll(payment.getStudentId(), payment.getCourseId());

        Course course = courseRepository.findById(payment.getCourseId()).orElse(null);
        log.info("Payment confirmed for transactionCode={}, student={}", transactionCode, payment.getStudentId());

        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .courseId(payment.getCourseId())
                .courseTitle(course != null ? course.getTitle() : null)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus("SUCCESS")
                .transactionCode(transactionCode)
                .paidAt(payment.getPaidAt())
                .message("Xác nhận thanh toán thành công! Bạn đã được đăng ký vào khóa học.")
                .build();
    }

    // ─── Lịch sử thanh toán ─────────────────────────────────────────────────────

    public List<PaymentResponse> getPaymentHistory(Integer studentId) {
        return paymentRepository.findByStudentIdOrderByPaidAtDesc(studentId)
                .stream()
                .map(p -> {
                    Course course = courseRepository.findById(p.getCourseId()).orElse(null);
                    return PaymentResponse.builder()
                            .paymentId(p.getPaymentId())
                            .courseId(p.getCourseId())
                            .courseTitle(course != null ? course.getTitle() : "Unknown")
                            .amount(p.getAmount())
                            .paymentMethod(p.getPaymentMethod())
                            .paymentStatus(p.getPaymentStatus())
                            .transactionCode(p.getTransactionCode())
                            .paidAt(p.getPaidAt())
                            .build();
                })
                .toList();
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    /**
     * Mock gateway logic.
     * Thực tế: gọi VNPAY/MOMO SDK tại đây, return status từ response.
     * FREE course → SUCCESS ngay.
     */
    private String processPaymentGateway(String method, String transactionCode, Course course) {
        if (course.getPrice() == null || course.getPrice().doubleValue() == 0) {
            return "SUCCESS"; // Khóa học miễn phí
        }
        if ("FREE".equalsIgnoreCase(method)) {
            return "SUCCESS";
        }
        // Với VNPAY/MOMO: client đã redirect và có transactionCode → xem là SUCCESS
        if (transactionCode != null && !transactionCode.startsWith("TXN-")) {
            return "SUCCESS";
        }
        // Chưa có transactionCode thật → PENDING (chờ callback)
        return "PENDING";
    }

    private void autoEnroll(Integer studentId, Integer courseId) {
        if (!enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            Enrollment enrollment = new Enrollment();
            enrollment.setStudentId(studentId);
            enrollment.setCourseId(courseId);
            enrollment.setEnrolledAt(new Date());
            enrollment.setProgressPercent(0.0);
            enrollmentRepository.save(enrollment);
        }
    }
}
