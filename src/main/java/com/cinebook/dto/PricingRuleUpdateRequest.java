package com.cinebook.dto;

import java.math.BigDecimal;

public record PricingRuleUpdateRequest(
    BigDecimal price
) {}
