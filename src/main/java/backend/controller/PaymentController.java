package backend.controller;

import backend.dto.request.PurchaseRequest;
import backend.dto.response.PaymentResponse;
import backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UC-14: Purchase Packages
 * Base URL: /api/payments
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Mua khóa học / gói đề thi
     * Body: { courseId, paymentMethod, transactionCode? }
     */
    @PostMapping("/purchase")
    public ResponseEntity<PaymentResponse> purchase(
            @RequestHeader("X-Student-Id") Integer studentId,
            @RequestBody PurchaseRequest request) {

        return ResponseEntity.ok(
                paymentService.purchaseCourse(studentId, request)
        );
    }

    @GetMapping("/history")
    public ResponseEntity<?> history(
            @RequestHeader("X-Student-Id") Integer studentId) {

        return ResponseEntity.ok(
                paymentService.getPaymentHistory(studentId)
        );
    }

    @PostMapping("/confirm/{transactionCode}")
    public ResponseEntity<?> confirm(@PathVariable String transactionCode) {
        return ResponseEntity.ok(
                paymentService.confirmPayment(transactionCode)
        );
    }

    @PostMapping("/bank/create")
    public ResponseEntity<?> createBank(@RequestBody PurchaseRequest request,
                                        @RequestHeader("X-Student-Id") Integer studentId) {

        return ResponseEntity.ok(paymentService.createBankPayment(studentId, request));
    }

    @PostMapping("/bank/confirm/{transactionCode}")
    public ResponseEntity<?> confirmBank(@PathVariable String transactionCode) {
        return ResponseEntity.ok(paymentService.confirmPayment(transactionCode));
    }
}
