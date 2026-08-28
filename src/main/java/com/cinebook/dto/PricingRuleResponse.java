package com.cinebook.dto;

import com.cinebook.entity.enums.TicketType;
import java.math.BigDecimal;

public record PricingRuleResponse(
    Long id,
    Long showtimeId,
    Long sectionId,
    TicketType ticketType,
    BigDecimal price
) {}
