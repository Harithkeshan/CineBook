package com.cinebook.controller;

import com.cinebook.dto.PricingRuleResponse;
import com.cinebook.entity.PricingRule;
import com.cinebook.service.PricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping("/showtimes/{showtimeId}/pricing")
    public ResponseEntity<List<PricingRuleResponse>> getPricingRulesByShowtimeId(@PathVariable Long showtimeId) {
        List<PricingRuleResponse> pricingRules = pricingService.getPricingRulesByShowtimeId(showtimeId).stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(pricingRules);
    }

    private PricingRuleResponse mapToResponse(PricingRule pricingRule) {
        return new PricingRuleResponse(
                pricingRule.getId(),
                pricingRule.getShowtime() != null ? pricingRule.getShowtime().getId() : null,
                pricingRule.getSection() != null ? pricingRule.getSection().getId() : null,
                pricingRule.getTicketType(),
                pricingRule.getPrice()
        );
    }
}
