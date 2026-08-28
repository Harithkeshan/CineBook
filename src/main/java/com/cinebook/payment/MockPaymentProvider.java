package com.cinebook.payment;

import com.cinebook.entity.Booking;
import com.cinebook.entity.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentProvider implements PaymentProvider {

    public static final String PROVIDER_NAME = "MOCK";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public InitiateResult initiatePayment(Booking booking, BigDecimal amount, String currency) {
        String transactionId = "MOCK-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new InitiateResult(transactionId, "http://localhost:8080/mock-payment/" + transactionId);
    }

    @Override
    public VerifyResult verifyPayment(Payment payment, boolean simulateSuccess) {
        String transactionId = payment.getProviderTransactionId() != null
                ? payment.getProviderTransactionId()
                : "MOCK-TXN-VERIFIED";

        if (simulateSuccess) {
            return new VerifyResult(true, transactionId, "Payment confirmed successfully by Mock Payment Provider");
        } else {
            return new VerifyResult(false, transactionId, "Payment declined by Mock Payment Provider");
        }
    }
}
