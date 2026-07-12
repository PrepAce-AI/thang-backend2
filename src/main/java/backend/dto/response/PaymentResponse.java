package backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class PaymentResponse {
    private Integer paymentId;
    private Integer courseId;
    private String courseTitle;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionCode;
    private Date paidAt;
    private String message;
}
