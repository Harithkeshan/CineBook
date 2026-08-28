package com.cinebook.dto;

import com.cinebook.entity.enums.ShowtimeSeatStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record SeatHoldResponse(
    Long showtimeId,
    List<Long> seatIds,
    ShowtimeSeatStatus status,
    OffsetDateTime holdExpiresAt
) {}
