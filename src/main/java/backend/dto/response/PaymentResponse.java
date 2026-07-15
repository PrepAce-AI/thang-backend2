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

    // Mã giao dịch phía ngân hàng (nếu có)
    private String bankTransactionId;

    // URL QR để frontend hiển thị
    private String qrUrl;

    // Nội dung chuyển khoản
    private String transferContent;

    private Date createdAt;

    private Date paidAt;

    private Date updatedAt;

    private String message;

    private String studentName;
}