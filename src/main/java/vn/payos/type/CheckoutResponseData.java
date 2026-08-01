package vn.payos.type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder = CheckoutResponseData.CheckoutResponseDataBuilder.class)
public class CheckoutResponseData {
    private String currency;
    private String status;
    private String accountNumber;
    private String bin;
    private String accountName;
    private String checkoutUrl;
    private long orderCode;
    private String description;
    private String qrCode;
    private int amount;
    private String paymentLinkId;
    private Long expiredAt;

    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    public static class CheckoutResponseDataBuilder {}
}