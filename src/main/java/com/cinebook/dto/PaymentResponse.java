package com.cinebook.dto;

import com.cinebook.entity.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentResponse(
    Long paymentId,
    String bookingReference,
    String provider,
    String providerTransactionId,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    OffsetDateTime createdAt
) {}
