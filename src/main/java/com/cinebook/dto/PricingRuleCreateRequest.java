package com.cinebook.dto;

import com.cinebook.entity.enums.TicketType;
import java.math.BigDecimal;

public record PricingRuleCreateRequest(
    Long sectionId,
    TicketType ticketType,
    BigDecimal price
) {}
