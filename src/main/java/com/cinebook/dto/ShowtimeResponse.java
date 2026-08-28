package com.cinebook.dto;

import com.cinebook.entity.enums.ShowtimeStatus;
import java.time.OffsetDateTime;

public record ShowtimeResponse(
    Long id,
    Long movieId,
    Long hallId,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    ShowtimeStatus status
) {}
