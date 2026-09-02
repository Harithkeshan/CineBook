package com.cinebook.dto;

import com.cinebook.entity.enums.BookingStatus;

public record CancelBookingResponse(
    String bookingReference,
    BookingStatus status,
    boolean refundIssued,
    String message
) {}
