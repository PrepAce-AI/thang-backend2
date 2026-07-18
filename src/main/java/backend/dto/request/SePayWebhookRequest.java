package backend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("content")
    private String content;

    /**
     * Số tiền SePay gửi về
     */
    @JsonProperty("transferAmount")
    private BigDecimal amount;

    /**
     * ID giao dịch của SEPay
     */
    @JsonProperty("id")
    private String bankTransactionId;

    /**
     * Thời gian chuyển khoản
     */
    @JsonProperty("transactionDate")
    private String transferTime;
}