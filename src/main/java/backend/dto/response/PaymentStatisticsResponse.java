package backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PaymentStatisticsResponse {

    private BigDecimal totalRevenue;

    private Integer totalOrders;

    private Integer totalStudents;

    private List<RecentPaymentDTO> recentPayments;

}