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
    public ResponseEntity<PaymentResponse> purchaseCourse(
            @RequestHeader("X-Student-Id") Integer studentId,
            @Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.ok(paymentService.purchaseCourse(studentId, request));
    }

    /**
     * Webhook callback từ VNPAY/MOMO xác nhận giao dịch
     * Gọi sau khi cổng thanh toán redirect về server
     */
    @PostMapping("/confirm/{transactionCode}")
    public ResponseEntity<PaymentResponse> confirmPayment(@PathVariable String transactionCode) {
        return ResponseEntity.ok(paymentService.confirmPayment(transactionCode));
    }

    /** Lịch sử giao dịch của học sinh */
    @GetMapping("/history")
    public ResponseEntity<List<PaymentResponse>> getHistory(
            @RequestHeader("X-Student-Id") Integer studentId) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(studentId));
    }
}
