package com.cinebook.dto;

import com.cinebook.entity.enums.TicketStatus;
import com.cinebook.entity.enums.TicketType;
import java.time.OffsetDateTime;

public record TicketCheckInResponse(
    boolean valid,
    String ticketNumber,
    String bookingReference,
    TicketStatus status,
    String seat,
    TicketType ticketType,
    OffsetDateTime usedAt
) {}
