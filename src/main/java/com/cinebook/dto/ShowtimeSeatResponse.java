package com.cinebook.dto;

import com.cinebook.entity.enums.SeatType;
import com.cinebook.entity.enums.SectionType;
import com.cinebook.entity.enums.ShowtimeSeatStatus;

public record ShowtimeSeatResponse(
    Long showtimeSeatId,
    Long seatId,
    Long sectionId,
    String sectionName,
    SectionType sectionType,
    String rowLabel,
    String seatNumber,
    SeatType seatType,
    Integer positionX,
    Integer positionY,
    ShowtimeSeatStatus status
) {}
