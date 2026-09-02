package com.cinebook.dto;

import com.cinebook.entity.enums.TicketStatus;
import com.cinebook.entity.enums.TicketType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TicketResponse(
    String ticketNumber,
    String bookingReference,
    Long seatId,
    String section,
    String rowLabel,
    String seatNumber,
    TicketType ticketType,
    BigDecimal price,
    TicketStatus status,
    OffsetDateTime issuedAt,
    String qrToken
) {}
