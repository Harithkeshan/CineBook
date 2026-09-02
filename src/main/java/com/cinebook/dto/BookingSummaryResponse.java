package com.cinebook.dto;

import com.cinebook.entity.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BookingSummaryResponse(
    String bookingReference,
    String movieTitle,
    OffsetDateTime showtimeStartTime,
    String locationName,
    String hallName,
    BookingStatus status,
    BigDecimal totalAmount,
    OffsetDateTime createdAt
) {}
