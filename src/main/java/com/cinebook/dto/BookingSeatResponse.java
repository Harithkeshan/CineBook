package com.cinebook.dto;

import com.cinebook.entity.enums.TicketType;
import java.math.BigDecimal;

public record BookingSeatResponse(
    Long seatId,
    String section,
    String rowLabel,
    String seatNumber,
    TicketType ticketType,
    BigDecimal price
) {}
