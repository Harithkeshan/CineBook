package com.cinebook.dto;

import com.cinebook.entity.enums.ShowtimeStatus;
import java.math.BigDecimal;

public record CancelShowtimeResponse(
    Long showtimeId,
    ShowtimeStatus status,
    int totalBookingsCancelled,
    int totalRefundsProcessed,
    BigDecimal totalRefundAmount,
    String message
) {}
