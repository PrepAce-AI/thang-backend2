package backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AdminPaymentDashboardResponse {

    private BigDecimal totalRevenue;

    private Long totalTransactions;

    private List<PaymentResponse> recentPayments;
}