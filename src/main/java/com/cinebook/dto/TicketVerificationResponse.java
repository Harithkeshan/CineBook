package com.cinebook.dto;

import com.cinebook.entity.enums.TicketStatus;
import com.cinebook.entity.enums.TicketType;

public record TicketVerificationResponse(
    boolean valid,
    String ticketNumber,
    TicketStatus status,
    String bookingReference,
    String seat,
    TicketType ticketType
) {}
