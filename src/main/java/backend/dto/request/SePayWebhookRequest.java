package backend.dto.request;

import lombok.Data;

@Data
public class SePayWebhookRequest {
    private String content;   // nội dung chuyển khoản
    private Long amount;
    private String gatewayTransactionId;
    private String transferTime;

    public SePayWebhookRequest(String content, Long amount, String gatewayTransactionId, String transferTime) {
        this.content = content;
        this.amount = amount;
        this.gatewayTransactionId = gatewayTransactionId;
        this.transferTime = transferTime;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getTransferTime() {
        return transferTime;
    }

    public void setTransferTime(String transferTime) {
        this.transferTime = transferTime;
    }
}
