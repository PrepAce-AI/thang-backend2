package backend.controller;
import backend.dto.request.SePayWebhookRequest;
import backend.service.PaymentService;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class SePayWebhookController {
    private final PaymentService paymentService;

    @PostMapping("/sepay")
    public ResponseEntity<?> handleSePay(@RequestBody SePayWebhookRequest request) {

        paymentService.handleSePayWebhook(request);

        return ResponseEntity.ok("OK");
    }
}
