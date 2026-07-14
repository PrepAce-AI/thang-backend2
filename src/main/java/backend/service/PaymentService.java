package backend.service;

import backend.dto.request.PurchaseRequest;
import backend.dto.request.SePayWebhookRequest;
import backend.dto.response.PaymentResponse;
import backend.entity.Course;
import backend.entity.Enrollment;
import backend.entity.Payment;
import backend.entity.User;
import backend.exceptions.BadRequestException;
import backend.exceptions.ResourceNotFoundException;
import backend.repository.CourseRepository;
import backend.repository.EnrollmentRepository;
import backend.repository.PaymentRepository;
import backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
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
    private final UserRepository userRepository;

    // ─── Mua khóa học ───────────────────────────────────────────────────────────

    @Transactional
    public PaymentResponse createBankPayment(
            Integer studentId,
            PurchaseRequest request) {

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy khóa học"));

        if (!Boolean.TRUE.equals(course.getIsPublished())) {
            throw new BadRequestException("Khóa học chưa được phát hành.");
        }

        if (paymentRepository.existsSuccessfulPayment(
                studentId,
                course.getCourseId())) {

            throw new BadRequestException("Bạn đã sở hữu khóa học này.");
        }

        if (enrollmentRepository.existsByStudentIdAndCourseId(
                studentId,
                course.getCourseId())) {

            throw new BadRequestException("Bạn đã đăng ký khóa học này.");
        }

        String transactionCode =
                "PAY-" + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();

        String transferContent = "PAY " + transactionCode;

        String bankBin = "970436";
        String accountNo = "9703391695";
        String accountName = "NGUYEN CUU THANG";

        String qrUrl =
                "https://img.vietqr.io/image/"
                        + bankBin
                        + "-"
                        + accountNo
                        + "-compact2.jpg"
                        + "?amount="
                        + course.getPrice().intValue()
                        + "&addInfo="
                        + transferContent
                        + "&accountName="
                        + accountName.replace(" ", "%20");

        Payment payment = new Payment();

        payment.setStudentId(studentId);
        payment.setCourseId(course.getCourseId());
        payment.setAmount(course.getPrice());

        payment.setPaymentMethod("BANK");
        payment.setPaymentStatus("PENDING");

        payment.setTransactionCode(transactionCode);

        payment.setCreatedAt(new Date());
        payment.setUpdatedAt(new Date());
        payment.setPaidAt(null);

        paymentRepository.save(payment);

        log.info(
                "Create payment: student={}, course={}, txn={}",
                studentId,
                course.getCourseId(),
                transactionCode
        );

        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .studentId(studentId)
                .courseId(course.getCourseId())
                .courseTitle(course.getTitle())
                .amount(course.getPrice())
                .paymentMethod("BANK")
                .paymentStatus("PENDING")
                .transactionCode(transactionCode)
                .transferContent(transferContent)
                .qrUrl(qrUrl)
                .createdAt(payment.getCreatedAt())
                .message("Đã tạo giao dịch. Vui lòng chuyển khoản đúng nội dung.")
                .build();
    }

    /**
     * ==========================================================
     * XÁC NHẬN THANH TOÁN
     * (Được gọi bởi SePay Webhook hoặc Admin)
     * ==========================================================
     */
    @Transactional(readOnly = true)
    public PaymentResponse confirmPayment(String transactionCode){

        Payment payment = paymentRepository
                .findByTransactionCode(transactionCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found"));

        Course course = courseRepository
                .findById(payment.getCourseId())
                .orElse(null);

        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .studentId(payment.getStudentId())
                .courseId(payment.getCourseId())
                .courseTitle(course!=null?course.getTitle():null)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .transactionCode(payment.getTransactionCode())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .message(payment.getPaymentStatus())
                .build();
    }
    @Transactional
    public PaymentResponse adminConfirmPayment(String transactionCode) {
        Payment payment = paymentRepository
                .findByTransactionCode(transactionCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy giao dịch: " + transactionCode
                        ));
        // 1. Không cho confirm lại
        if ("SUCCESS".equals(payment.getPaymentStatus())) {
            throw new BadRequestException(
                    "Giao dịch này đã được xác nhận trước đó."
            );
        }
        // 2. Chỉ cho phép confirm payment đang chờ
        if (!"WAITING_CONFIRM".equals(payment.getPaymentStatus())) {
            throw new BadRequestException(
                    "Giao dịch không ở trạng thái chờ xác nhận."
            );
        }
        // 3. Check student tồn tại
        User student = userRepository
                .findById(payment.getStudentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy học viên."
                        ));
        // 4. Check course tồn tại
        Course course = courseRepository
                .findById(payment.getCourseId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy khóa học."
                        ));
        // 5. Check payment amount
        if (payment.getAmount() == null ||
                course.getPrice() == null ||
                payment.getAmount()
                        .compareTo(course.getPrice()) != 0) {
            throw new BadRequestException(
                    "Số tiền thanh toán không khớp với khóa học."
            );
        }
        // 6. Check thời gian thanh toán
        long elapsed =
                new Date().getTime()
                        -
                        payment.getCreatedAt().getTime();
        // quá 24h thì không cho xác nhận
        if (elapsed > 24 * 60 * 60 * 1000) {
            throw new BadRequestException(
                    "Giao dịch đã hết hạn xác nhận."
            );
        }
        // 7. Update payment
        payment.setPaymentStatus("SUCCESS");
        payment.setPaidAt(new Date());
        payment.setUpdatedAt(new Date());
        paymentRepository.save(payment);

        // 8. Mở khóa khóa học
        autoEnroll(
                payment.getStudentId(),
                payment.getCourseId()
        );
        log.info(
                "ADMIN CONFIRM PAYMENT SUCCESS - Student={}, Course={}, Txn={}",
                payment.getStudentId(),
                payment.getCourseId(),
                transactionCode
        );
        // 9. Response
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .studentId(payment.getStudentId())
                .studentName(student.getFullName())
                .courseId(payment.getCourseId())
                .courseTitle(course.getTitle())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .transactionCode(payment.getTransactionCode())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .message(
                        "Admin đã xác nhận thanh toán thành công."
                )
                .build();
    }

    @Transactional
    public PaymentResponse cancelPayment(String transactionCode){

        Payment payment = paymentRepository
                .findByTransactionCode(transactionCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy giao dịch: "
                                        + transactionCode
                        )
                );


        if(!"WAITING_CONFIRM".equals(payment.getPaymentStatus())){
            throw new BadRequestException(
                    "Chỉ được hủy giao dịch đang chờ xác nhận."
            );
        }


        payment.setPaymentStatus("CANCELLED");
        payment.setUpdatedAt(new Date());

        paymentRepository.save(payment);


        Course course = courseRepository
                .findById(payment.getCourseId())
                .orElse(null);


        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .studentId(payment.getStudentId())
                .courseId(payment.getCourseId())
                .courseTitle(
                        course != null
                                ? course.getTitle()
                                : null
                )
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .transactionCode(payment.getTransactionCode())
                .createdAt(payment.getCreatedAt())
                .message(
                        "Admin đã hủy giao dịch."
                )
                .build();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPendingPayments() {

        return paymentRepository.findByPaymentStatus("WAITING_CONFIRM")
                .stream()
                .map(payment -> {

                    Course course = courseRepository
                            .findById(payment.getCourseId())
                            .orElse(null);

                    User student = userRepository
                            .findById(payment.getStudentId())
                            .orElse(null);

                    return PaymentResponse.builder()
                            .paymentId(payment.getPaymentId())

                            // Student info
                            .studentId(payment.getStudentId())
                            .studentName(
                                    student != null
                                            ? student.getFullName()
                                            : "Unknown"
                            )

                            // Course info
                            .courseId(payment.getCourseId())
                            .courseTitle(
                                    course != null
                                            ? course.getTitle()
                                            : "Unknown"
                            )

                            // Payment info
                            .amount(payment.getAmount())
                            .paymentMethod(payment.getPaymentMethod())
                            .paymentStatus(payment.getPaymentStatus())
                            .transactionCode(payment.getTransactionCode())
                            .createdAt(payment.getCreatedAt())
                            .paidAt(payment.getPaidAt())

                            .message("Đang chờ xác nhận thanh toán")
                            .build();
                })
                .toList();
    }

    /**
     * ==========================================================
     * LỊCH SỬ THANH TOÁN
     * ==========================================================
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentHistory(Integer studentId) {

        return paymentRepository
                .findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(payment -> {

                    Course course = courseRepository
                            .findById(payment.getCourseId())
                            .orElse(null);

                    return PaymentResponse.builder()
                            .paymentId(payment.getPaymentId())
                            .studentId(payment.getStudentId())
                            .courseId(payment.getCourseId())
                            .courseTitle(course != null
                                    ? course.getTitle()
                                    : "Unknown")
                            .amount(payment.getAmount())
                            .paymentMethod(payment.getPaymentMethod())
                            .paymentStatus(payment.getPaymentStatus())
                            .transactionCode(payment.getTransactionCode())
                            .createdAt(payment.getCreatedAt())
                            .paidAt(payment.getPaidAt())
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

    /**
     * ==========================================================
     * WEBHOOK TỪ SePay
     * ==========================================================
     */
    @Transactional
    public void handleSePayWebhook(SePayWebhookRequest req) {

        log.info("===== SEPAY WEBHOOK =====");
        log.info("Content      : {}", req.getContent());
        log.info("Amount       : {}", req.getAmount());
        log.info("Bank Txn Id  : {}", req.getBankTransactionId());

        // Nội dung chuyển khoản
        String content = req.getContent();

        if (content == null || content.isBlank()) {
            log.warn("Webhook không có nội dung chuyển khoản");
            return;
        }

        // Tìm payment theo transactionCode
        Payment payment = paymentRepository
                .findByTransactionCodeContaining(content)
                .orElse(null);

        if (payment == null) {
            log.warn("Không tìm thấy payment với content={}", content);
            return;
        }

        // Đã xử lý rồi
        if ("SUCCESS".equals(payment.getPaymentStatus())) {
            log.info("Payment {} đã SUCCESS trước đó", payment.getTransactionCode());
            return;
        }

        // Kiểm tra số tiền
        if (req.getAmount() == null ||
                payment.getAmount().compareTo(req.getAmount()) != 0) {

            log.warn("Sai số tiền. DB={} Webhook={}",
                    payment.getAmount(),
                    req.getAmount());

            return;
        }

        // Cập nhật payment
        payment.setPaymentStatus("SUCCESS");
        payment.setPaidAt(new Date());
        payment.setUpdatedAt(new Date());
        payment.setBankTransactionId(req.getBankTransactionId());

        paymentRepository.save(payment);

        // Auto enroll
        autoEnroll(payment.getStudentId(), payment.getCourseId());

        log.info("Payment {} SUCCESS",
                payment.getTransactionCode());

        log.info("=========================");
    }

    @Transactional
    public PaymentResponse purchaseCourse(Integer studentId, PurchaseRequest request) {
        String method = request.getPaymentMethod().toUpperCase();
        switch (method) {
            case "BANK":
                return createBankPayment(studentId, request);
            // Sau này mở rộng
            // case "VNPAY":
            //     return createVnPayPayment(studentId, request);

            // case "MOMO":
            //     return createMoMoPayment(studentId, request);

            default:
                throw new BadRequestException("Unsupported payment method: " + method);
        }
    }

    /**
     * ==========================================================
     * KIỂM TRA TRẠNG THÁI GIAO DỊCH
     * ==========================================================
     */
    @Transactional(readOnly = true)
    public PaymentResponse checkPaymentStatus(String transactionCode) {

        Payment payment = paymentRepository
                .findByTransactionCode(transactionCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy giao dịch"));

        Course course = courseRepository
                .findById(payment.getCourseId())
                .orElse(null);

        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .studentId(payment.getStudentId())
                .courseId(payment.getCourseId())
                .courseTitle(course != null ? course.getTitle() : null)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .transactionCode(payment.getTransactionCode())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .message(payment.getPaymentStatus())
                .build();
    }

    // -------------------- STATUS BANKING -----------------------
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(String transactionCode) {

        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found"));

        Course course = courseRepository.findById(payment.getCourseId())
                .orElse(null);

        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .studentId(payment.getStudentId())
                .courseId(payment.getCourseId())
                .courseTitle(course != null ? course.getTitle() : "")
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .transactionCode(payment.getTransactionCode())
                .transferContent(payment.getTransactionCode())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .message(payment.getPaymentStatus())
                .build();
    }

    // -------------------- WAITING BANKING -----------------------

    @Transactional
    public PaymentResponse waitingConfirm(String transactionCode){

        Payment payment = paymentRepository
                .findByTransactionCode(transactionCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy giao dịch"
                        ));
        // Chỉ PENDING mới được gửi yêu cầu
        if(!"PENDING".equals(payment.getPaymentStatus())){

            throw new BadRequestException(
                    "Giao dịch này đã được gửi xác nhận hoặc đã hoàn tất."
            );
        }

        payment.setPaymentStatus("WAITING_CONFIRM");
        payment.setUpdatedAt(new Date());

        paymentRepository.save(payment);
        log.info(
                "Payment waiting confirm: {}",
                transactionCode
        );
        return getPaymentStatus(transactionCode);
    }
}
