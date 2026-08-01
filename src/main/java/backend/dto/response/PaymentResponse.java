package backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class PaymentResponse {

    private Integer paymentId;

    private Integer studentId;

    private Integer courseId;

    private String courseTitle;

    private BigDecimal amount;

    // BANK | MOMO | VNPAY | ZALOPAY
    private String paymentMethod;

    // PENDING | SUCCESS | FAILED | EXPIRED
    private String paymentStatus;

    private String transactionCode;

    // PayOS orderCode (= transactionCode as Long)
    private Long orderCode;

    // Mã giao dịch phía ngân hàng (nếu có)
    private String bankTransactionId;

    // URL QR để frontend hiển thị
    private String qrUrl;

    // URL checkout của PayOS
    private String checkoutUrl;

    // Nội dung chuyển khoản
    private String transferContent;

    private String accountNumber;
    private String accountName;
    private String bin;
    private String description;

    private Date createdAt;

    private Date paidAt;

    private Date updatedAt;

    private String message;

    private String studentName;
}