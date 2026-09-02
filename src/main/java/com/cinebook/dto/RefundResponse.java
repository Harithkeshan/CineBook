package com.cinebook.dto;

import com.cinebook.entity.enums.RefundReason;
import com.cinebook.entity.enums.RefundStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RefundResponse(
    Long refundId,
    String bookingReference,
    Long paymentId,
    BigDecimal amount,
    RefundReason reason,
    RefundStatus status,
    String providerRefundId,
    OffsetDateTime createdAt
) {}
