package backend.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;


@Data
public class RecentPaymentDTO {

    private Integer paymentId;

    private String studentName;

    private String courseTitle;

    private BigDecimal amount;

    private String paymentStatus;

    private Date paidAt;

}