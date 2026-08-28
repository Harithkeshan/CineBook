package com.cinebook.dto;

import com.cinebook.entity.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record BookingResponse(
    String bookingReference,
    Long showtimeId,
    String customerName,
    String customerEmail,
    String customerPhone,
    BookingStatus status,
    BigDecimal totalAmount,
    OffsetDateTime createdAt,
    List<BookingSeatResponse> seats
) {}
