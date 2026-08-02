package backend.service;

import backend.dto.request.PurchaseRequest;
import backend.dto.request.SePayWebhookRequest;
import backend.dto.response.AdminPaymentDashboardResponse;
import backend.dto.response.PaymentResponse;
import backend.entity.*;
import backend.exceptions.BadRequestException;
import backend.exceptions.ResourceNotFoundException;
import backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UC-14: Purchase Packages
 * Xử lý giao dịch mua khóa học. Sau khi thanh toán thành công → tự động enroll.
 * Kiến trúc hiện tại: mock/simulate gateway, dễ tích hợp VNPAY/MOMO sau.
 */
import vn.payos.PayOS;
import vn.payos.type.PaymentData;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PayOS payOS;

    @Value("${sepay.api-token}")
    private String apiToken;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

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

        long orderCode = Long.parseLong(String.valueOf(System.currentTimeMillis()).substring(3, 13));
        String transactionCode = String.valueOf(orderCode);

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

        String checkoutUrl = "";
        try {
            ItemData item = ItemData.builder()
                    .name(course.getTitle())
                    .price(course.getPrice().intValue())
                    .quantity(1)
                    .build();

            String returnUrl = frontendUrl + "/payment/return";
            String cancelUrl = frontendUrl + "/checkout/" + course.getCourseId();

            PaymentData paymentData = PaymentData.builder()
                    .orderCode(orderCode)
                    .amount(course.getPrice().intValue())
                    .description("Thanh toan khoa hoc")
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .item(item)
                    .build();

            CheckoutResponseData data = payOS.createPaymentLink(paymentData);
            checkoutUrl = data.getCheckoutUrl();

            // Lấy thêm thông tin cho frontend
            String accountNumber = data.getAccountNumber();
            String accountName = data.getAccountName();
            String bin = data.getBin();
            String description = data.getDescription();

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
                    .orderCode(orderCode)
                    .checkoutUrl(checkoutUrl)
                    .accountNumber(accountNumber)
                    .accountName(accountName)
                    .bin(bin)
                    .description(description)
                    .createdAt(payment.getCreatedAt())
                    .message("Đã tạo giao dịch.")
                    .build();
        } catch (Exception e) {
            log.error("Failed to create PayOS payment link: ", e);
            throw new BadRequestException("Lỗi PayOS: " + e.getMessage());
        }
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
        Course course = courseRepository
                .findById(payment.getCourseId())
                .orElse(null);

        paymentRepository.save(payment);

        createNotification(
                payment.getStudentId(),
                "Thanh toán bị từ chối",
                "Thanh toán khóa học \""
                        + course.getTitle()
                        + "\" đã bị Admin từ chối. Nếu bạn đã chuyển khoản, vui lòng liên hệ bộ phận hỗ trợ."
        );
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

    private void createNotification(
            Integer userId,
            String title,
            String content) {

        Notification notification = new Notification();

        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetRole("USER");
        notification.setReceiverId(userId);

        // hoặc ADMIN nếu muốn biết ai duyệt
        notification.setCreatedBy(null);

        notification.setCreatedAt(new Date());

        notificationRepository.save(notification);
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                notification
        );
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

        // Trích xuất mã giao dịch có dạng PAY[A-F0-9]{8} hoặc PAY-[A-F0-9]{8}
        String txnCode = null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("PAY-?[A-F0-9]{8}", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            txnCode = matcher.group().toUpperCase();
            if (txnCode.contains("-")) {
                txnCode = txnCode.replace("-", "");
            }
        }

        if (txnCode == null) {
            log.warn("Không tìm thấy mã giao dịch trong nội dung: {}", content);
            return;
        }

        // Tìm payment theo transactionCode
        Payment payment = paymentRepository
                .findByTransactionCode(txnCode)
                .orElse(null);

        // Hỗ trợ tìm ngược lại nếu DB lưu có dấu gạch ngang (VD: giao dịch cũ PAY-XXXX)
        if (payment == null && !txnCode.contains("-")) {
            String legacyTxnCode = txnCode.replace("PAY", "PAY-");
            payment = paymentRepository.findByTransactionCode(legacyTxnCode).orElse(null);
        }

        Course course = null;

        if (payment == null) {
            log.warn("Không tìm thấy payment với content={}", content);
            return;
        }

        course = courseRepository
                .findById(payment.getCourseId())
                .orElse(null);

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
        createNotification(
                payment.getStudentId(),
                "Thanh toán thành công",
                "Thanh toán khóa học \""
                        + (course != null ? course.getTitle() : "")
                        + "\" đã được Admin xác nhận. Bạn có thể bắt đầu học ngay."
        );

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

    // -------------------- STATUS BANKING (WITH SEPAY API POLLING) -----------------------
    @Transactional
    public PaymentResponse getPaymentStatus(String transactionCode) {

        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found"));

        Course course = courseRepository.findById(payment.getCourseId())
                .orElse(null);

        // NẾU ĐƠN HÀNG ĐANG CHỜ, CHỦ ĐỘNG GỌI PAYOS/SEPAY API ĐỂ KIỂM TRA MÀ KHÔNG CẦN WEBHOOK
        if ("PENDING".equals(payment.getPaymentStatus()) || "WAITING_CONFIRM".equals(payment.getPaymentStatus())) {
            // 1. CHỦ ĐỘNG KIỂM TRA TRẠNG THÁI PAYOS TRƯỚC
            try {
                long orderCode = Long.parseLong(payment.getTransactionCode());
                vn.payos.type.PaymentLinkData linkData = payOS.getPaymentLinkInformation(orderCode);

                if (linkData != null && "PAID".equals(linkData.getStatus())) {
                    payment.setPaymentStatus("SUCCESS");
                    payment.setPaidAt(new Date());
                    payment.setUpdatedAt(new Date());
                    payment.setBankTransactionId(linkData.getId()); // ID giao dịch của PayOS
                    paymentRepository.save(payment);

                    // Mở khóa học
                    autoEnroll(payment.getStudentId(), payment.getCourseId());
                    createNotification(
                            payment.getStudentId(),
                            "Thanh toán thành công",
                            "Thanh toán khóa học \""
                                    + (course != null ? course.getTitle() : "")
                                    + "\" đã được hệ thống xác nhận. Bạn có thể bắt đầu học ngay."
                    );

                    log.info("PAYOS API POLLING SUCCESS: Payment {}", payment.getTransactionCode());
                }
            } catch (Exception ex) {
                log.debug("PAYOS POLLING: Không thể check status PayOS cho đơn hàng {} - {}", payment.getTransactionCode(), ex.getMessage());
            }

            // 2. NẾU VẪN PENDING THÌ KIỂM TRA SEPAY (Dành cho các đơn SePay cũ nếu có)
            if ("PENDING".equals(payment.getPaymentStatus()) || "WAITING_CONFIRM".equals(payment.getPaymentStatus())) {
                try {
                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + apiToken);
                    headers.set("Content-Type", "application/json");
                    HttpEntity<String> entity = new HttpEntity<>(headers);

                    ResponseEntity<Map> response = restTemplate.exchange(
                            "https://my.sepay.vn/userapi/transactions/list",
                            HttpMethod.GET,
                            entity,
                            Map.class
                    );

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        Map<String, Object> body = response.getBody();
                        if (body.containsKey("transactions")) {
                            List<Map<String, Object>> transactions = (List<Map<String, Object>>) body.get("transactions");

                            // Lọc qua danh sách giao dịch gần nhất
                            for (Map<String, Object> txn : transactions) {
                                String content = txn.get("transaction_content") != null ? txn.get("transaction_content").toString() : "";
                                String amountStr = txn.get("amount_in") != null ? txn.get("amount_in").toString() : "0";
                                double amountIn = Double.parseDouble(amountStr);

                                // Bỏ gạch ngang để so khớp linh hoạt
                                String cleanContent = content.replace("-", "").toUpperCase();
                                String cleanTxnCode = payment.getTransactionCode().replace("-", "").toUpperCase();

                                if (cleanContent.contains(cleanTxnCode)) {
                                    // Kiểm tra số tiền khớp 100%
                                    if (payment.getAmount() != null && payment.getAmount().doubleValue() <= amountIn) {
                                        // CHỐT ĐƠN!
                                        payment.setPaymentStatus("SUCCESS");
                                        payment.setPaidAt(new Date());
                                        payment.setUpdatedAt(new Date());
                                        payment.setBankTransactionId(txn.get("reference_number") != null ? txn.get("reference_number").toString() : "API_SYNC");
                                        paymentRepository.save(payment);

                                        // Mở khóa học
                                        autoEnroll(payment.getStudentId(), payment.getCourseId());
                                        createNotification(
                                                payment.getStudentId(),
                                                "Thanh toán thành công",
                                                "Thanh toán khóa học \""
                                                        + (course != null ? course.getTitle() : "")
                                                        + "\" đã được Admin xác nhận. Bạn có thể bắt đầu học ngay."
                                        );

                                        log.info("API POLLING SUCCESS: Tìm thấy giao dịch {} cho Payment {}", cleanContent, payment.getTransactionCode());
                                        break;
                                    } else {
                                        log.warn("API POLLING: Tìm thấy mã {} nhưng số tiền không khớp (Thực tế: {}, Yêu cầu: {})", cleanTxnCode, amountIn, payment.getAmount());
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Lỗi khi chủ động gọi SePay API: {}", e.getMessage());
                }
            }
        }

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

    // STATISTICS
    @Transactional(readOnly = true)
    public AdminPaymentDashboardResponse getAdminDashboard(){
        BigDecimal revenue =
                paymentRepository.getTotalRevenue();
        Long total =
                paymentRepository.countSuccessfulPayments();
        List<PaymentResponse> recent =
                paymentRepository
                        .findTop10ByPaymentStatusOrderByPaidAtDesc("SUCCESS")
                        .stream()
                        .map(payment -> {
                            Course course =
                                    courseRepository
                                            .findById(payment.getCourseId())
                                            .orElse(null);


                            User student =
                                    userRepository
                                            .findById(payment.getStudentId())
                                            .orElse(null);


                            return PaymentResponse.builder()
                                    .paymentId(payment.getPaymentId())
                                    .studentId(payment.getStudentId())
                                    .studentName(
                                            student != null ?
                                                    student.getFullName()
                                                    :
                                                    "Unknown"
                                    )

                                    .courseId(payment.getCourseId())

                                    .courseTitle(
                                            course != null ?
                                                    course.getTitle()
                                                    :
                                                    "Unknown"
                                    )

                                    .amount(payment.getAmount())

                                    .paymentStatus(
                                            payment.getPaymentStatus()
                                    )

                                    .transactionCode(
                                            payment.getTransactionCode()
                                    )

                                    .paidAt(
                                            payment.getPaidAt()
                                    )

                                    .build();

                        })
                        .toList();

        return AdminPaymentDashboardResponse.builder()
                .totalRevenue(revenue)
                .totalTransactions(total)
                .recentPayments(recent)
                .build();
    }

    @Transactional
    public void handlePayOSWebhook(vn.payos.type.WebhookData data) {
        String transactionCode = String.valueOf(data.getOrderCode());

        Payment payment = paymentRepository.findByTransactionCode(transactionCode).orElse(null);

        if (payment == null) {
            log.warn("Không tìm thấy payment với orderCode={}", data.getOrderCode());
            return;
        }

        Course course = courseRepository.findById(payment.getCourseId()).orElse(null);

        if ("SUCCESS".equals(payment.getPaymentStatus())) {
            log.info("Payment {} đã SUCCESS trước đó", transactionCode);
            return;
        }

        if (data.getAmount() == 0 || payment.getAmount().compareTo(java.math.BigDecimal.valueOf(data.getAmount())) != 0) {
            log.warn("Sai số tiền. DB={} Webhook={}", payment.getAmount(), data.getAmount());
            return;
        }

        payment.setPaymentStatus("SUCCESS");
        payment.setPaidAt(new Date());
        payment.setUpdatedAt(new Date());
        payment.setBankTransactionId(data.getReference());

        paymentRepository.save(payment);

        autoEnroll(payment.getStudentId(), payment.getCourseId());
        createNotification(
                payment.getStudentId(),
                "Thanh toán thành công",
                "Thanh toán khóa học \"" + (course != null ? course.getTitle() : "") + "\" đã được xác nhận. Bạn có thể bắt đầu học ngay."
        );

        log.info("Payment {} SUCCESS", transactionCode);
    }
}