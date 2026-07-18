package backend.controller;
import backend.dto.request.SePayWebhookRequest;
import backend.service.PaymentService;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class SePayWebhookController {
    private final PaymentService paymentService;

    @Value("${sepay.api-key}")
    private String apiKey;

    @PostMapping("/sepay")
    public ResponseEntity<?> handleSePay(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SePayWebhookRequest request) {

        if (authHeader == null || !authHeader.startsWith("Apikey ") || !authHeader.substring(7).trim().equals(apiKey)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        paymentService.handleSePayWebhook(request);

        return ResponseEntity.ok("OK");
    }
}
