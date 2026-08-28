package com.cinebook.dto;

import com.cinebook.entity.enums.TicketType;
import jakarta.validation.constraints.NotNull;

public record BookingSeatRequest(
    @NotNull(message = "seatId is required")
    Long seatId,

    @NotNull(message = "ticketType is required")
    TicketType ticketType
) {}
