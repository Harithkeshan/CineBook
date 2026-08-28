package com.cinebook.dto;

import com.cinebook.entity.enums.SeatType;

public record SeatResponse(
    Long id,
    Long sectionId,
    String rowLabel,
    String seatNumber,
    SeatType seatType,
    Integer positionX,
    Integer positionY,
    Boolean isActive
) {}
