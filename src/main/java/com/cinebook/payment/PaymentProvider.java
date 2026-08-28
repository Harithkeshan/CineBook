package com.cinebook.payment;

import com.cinebook.entity.Booking;
import com.cinebook.entity.Payment;
import java.math.BigDecimal;

public interface PaymentProvider {
    
    String getProviderName();

    InitiateResult initiatePayment(Booking booking, BigDecimal amount, String currency);

    VerifyResult verifyPayment(Payment payment, boolean simulateSuccess);

    record InitiateResult(
        String transactionId,
        String redirectUrl
    ) {}

    record VerifyResult(
        boolean isSuccess,
        String transactionId,
        String message
    ) {}
}
