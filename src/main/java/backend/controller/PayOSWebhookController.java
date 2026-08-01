package backend.controller;

import backend.service.PaymentService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.PayOS;
import vn.payos.type.WebhookData;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook/payos")
@RequiredArgsConstructor
@Slf4j
public class PayOSWebhookController {

    private final PaymentService paymentService;
    private final PayOS payOS;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<?> handleWebhook(@RequestBody ObjectNode requestBody) {
        try {
            vn.payos.type.Webhook webhookBody = objectMapper.treeToValue(requestBody, vn.payos.type.Webhook.class);
            WebhookData data = payOS.verifyPaymentWebhookData(webhookBody);
            if (data != null) {
                paymentService.handlePayOSWebhook(data);
            }
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Webhook verification failed: ", e);
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
