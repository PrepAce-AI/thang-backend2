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

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class SePayWebhookController {
    private final PaymentService paymentService;
    
    // Khóa API SePay của bạn
    private static final String SEPAY_API_KEY = "2QA7PYLJECXFM9EHNAI1N2ZKIFKHSCKGQYYYAJI3ZHWRDJLKOWJVV8T09TEMO6N0";

    @PostMapping("/sepay")
    public ResponseEntity<?> handleSePay(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SePayWebhookRequest request) {

        // Validate API Key
        if (authHeader == null || !authHeader.equals("Apikey " + SEPAY_API_KEY)) {
            return ResponseEntity.status(403).body("Invalid API Key");
        }

        paymentService.handleSePayWebhook(request);

        return ResponseEntity.ok("OK");
    }
}

