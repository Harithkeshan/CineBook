package com.cinebook.dto;

import java.time.OffsetDateTime;

public record ShowtimeCreateRequest(
    Long movieId,
    Long hallId,
    OffsetDateTime startTime,
    OffsetDateTime endTime
) {}
