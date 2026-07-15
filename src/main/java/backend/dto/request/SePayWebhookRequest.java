package backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SePayWebhookRequest {

    /**
     * Nội dung chuyển khoản
     * Ví dụ: PAY-ABCD1234
     */
    private String content;

    /**
     * Số tiền SePay gửi về
     */
    private BigDecimal amount;

    /**
     * Mã giao dịch của ngân hàng
     */
    private String bankTransactionId;

    /**
     * Thời gian chuyển khoản
     */
    private String transferTime;
}