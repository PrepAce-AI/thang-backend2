package backend.controller;
import backend.dto.request.SePayWebhookRequest;
import backend.service.PaymentService;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class SePayWebhookController {
    private final PaymentService paymentService;
    
    @Value("${sepay.api-key}")
    private String sepayApiKey;

    @PostMapping("/sepay")
    public ResponseEntity<?> handleSePay(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SePayWebhookRequest request) {

        // Validate API Key
        if (authHeader == null || !authHeader.equals("Apikey " + sepayApiKey)) {
            return ResponseEntity.status(403).body("Invalid API Key");
        }

        paymentService.handleSePayWebhook(request);

        return ResponseEntity.ok("OK");
    }
}

