
package com.servicehub.serv.controller;

import com.razorpay.RazorpayException;
import com.servicehub.serv.service.PaymentService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Create a Razorpay order for a customer's booking.
    @PostMapping("/customer/payments/razorpay/order/{bookingId}")
    public ResponseEntity<Map<String, Object>> createRazorpayOrder(
            @PathVariable UUID bookingId,
            Authentication authentication) throws RazorpayException {

        UUID customerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                paymentService.createOrder(customerId, bookingId));
    }

    // Verify Razorpay payment after the checkout process.
    @PostMapping("/customer/payments/razorpay/verify")
    public ResponseEntity<Map<String, Object>> verifyRazorpayPayment(
            @RequestBody RazorpayVerificationRequest request,
            Authentication authentication) throws RazorpayException {

        UUID customerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                paymentService.verifyRazorpayPayment(
                        customerId,
                        request.razorpayOrderId(),
                        request.razorpayPaymentId(),
                        request.razorpaySignature()));
    }

    // Customer selects cash as the payment method.
    @PostMapping("/customer/payments/cash/{bookingId}")
    public ResponseEntity<Map<String, Object>> createCashPayment(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                paymentService.createCashPayment(customerId, bookingId));
    }

    // Customer cancels a pending payment selection to change methods.
    @PostMapping("/customer/payments/cancel/{bookingId}")
    public ResponseEntity<Void> cancelPendingPayment(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());

        paymentService.cancelPendingPayment(customerId, bookingId);

        return ResponseEntity.noContent().build();
    }

    // Assigned worker confirms that cash was actually received.
    @PostMapping("/worker/payments/cash/{bookingId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmCashPayment(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        UUID workerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                paymentService.confirmCashPayment(workerId, bookingId));
    }

    // Request body for Razorpay verification.
    public record RazorpayVerificationRequest(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature) {
    }
}
