package com.cinebook.controller;

import com.cinebook.dto.ConfirmPaymentRequest;
import com.cinebook.dto.InitiatePaymentRequest;
import com.cinebook.dto.PaymentResponse;
import com.cinebook.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/bookings/{bookingReference}/payment")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @PathVariable String bookingReference,
            @RequestBody(required = false) InitiatePaymentRequest request
    ) {
        PaymentResponse response = paymentService.initiatePayment(bookingReference, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/payments/{paymentId}/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @PathVariable Long paymentId,
            @RequestBody(required = false) ConfirmPaymentRequest request
    ) {
        PaymentResponse response = paymentService.confirmPayment(paymentId, request);
        return ResponseEntity.ok(response);
    }
}
