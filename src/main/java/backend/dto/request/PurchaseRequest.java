package backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PurchaseRequest {

    @NotNull(message = "courseId is required")
    private Integer courseId;


    @NotBlank(message = "paymentMethod is required")
    private String paymentMethod;

    private String transactionCode;
}
