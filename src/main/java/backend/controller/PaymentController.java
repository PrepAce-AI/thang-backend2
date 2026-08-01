package backend.controller;

import backend.dto.request.PurchaseRequest;
import backend.dto.request.SePayWebhookRequest;
import backend.dto.response.PaymentResponse;
import backend.entity.User;
import backend.repository.UserRepository;
import backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser(Authentication authentication) {

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    /**
     * Mua khóa học / gói đề thi
     * Body: { courseId, paymentMethod, transactionCode? }
     */
    @PostMapping("/purchase")
    public ResponseEntity<PaymentResponse> purchase(
            Authentication authentication,
            @RequestBody PurchaseRequest request) {

        User user = getCurrentUser(authentication);

        return ResponseEntity.ok(
                paymentService.purchaseCourse(user.getId(), request)
        );
    }

    @GetMapping("/history")
    public ResponseEntity<?> history(Authentication authentication) {

        User user = getCurrentUser(authentication);

        return ResponseEntity.ok(
                paymentService.getPaymentHistory(user.getId())
        );
    }

//    @PostMapping("/confirm/{transactionCode}")
//    public ResponseEntity<?> confirm(@PathVariable String transactionCode) {
//        return ResponseEntity.ok(
//                paymentService.confirmPayment(transactionCode)
//        );
//    }

    @PostMapping("/bank/create")
    public ResponseEntity<?> createBank(
            Authentication authentication,
            @RequestBody PurchaseRequest request) {

        try {
            String email = authentication.getName();
            System.out.println(email);
            User user = userRepository.findByEmail(email).orElseThrow();
            return ResponseEntity.ok(
                    paymentService.createBankPayment(user.getId(), request)
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/status/{transactionCode}")
    public ResponseEntity<PaymentResponse> getStatus(
            @PathVariable String transactionCode) {

        return ResponseEntity.ok(
                paymentService.getPaymentStatus(transactionCode)
        );
    }

//    @PostMapping("/bank/confirm/{transactionCode}")
//    public ResponseEntity<?> confirmBank(@PathVariable String transactionCode) {
//        return ResponseEntity.ok(paymentService.confirmPayment(transactionCode));
//    }

    @PostMapping("/waiting/{transactionCode}")
    public ResponseEntity<?> waiting(
            @PathVariable String transactionCode){
        System.out.println(
                "========== WAITING API HIT =========="
        );

        System.out.println(transactionCode);

        return ResponseEntity.ok(
                paymentService.waitingConfirm(transactionCode)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/confirm/{transactionCode}")
    public ResponseEntity<?> adminConfirm(
            @PathVariable String transactionCode){

        return ResponseEntity.ok(
                paymentService.adminConfirmPayment(transactionCode)
        );
    }

    @PostMapping("/admin/cancel/{transactionCode}")
    public ResponseEntity<?> cancelPayment(
            @PathVariable String transactionCode){

        return ResponseEntity.ok(
                paymentService.cancelPayment(transactionCode)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/pending")
    public ResponseEntity<List<PaymentResponse>> getPendingPayments(){

        return ResponseEntity.ok(
                paymentService.getPendingPayments()
        );
    }

//    @PostMapping("/webhook/sepay")
//    public ResponseEntity<String> webhook(
//            @RequestBody SePayWebhookRequest request){
//
//        paymentService.handleSePayWebhook(request);
//
//        return ResponseEntity.ok("OK");
//    }

    //STATISTICS
    @GetMapping("/admin/dashboard")
    public ResponseEntity<?> getAdminDashboard(){
        return ResponseEntity.ok(
                paymentService.getAdminDashboard()
        );
    }
}
