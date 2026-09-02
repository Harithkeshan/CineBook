package com.cinebook.dto;

import java.time.OffsetDateTime;

public record ShowtimeUpdateRequest(
    Long movieId,
    Long hallId,
    OffsetDateTime startTime,
    OffsetDateTime endTime
) {}
