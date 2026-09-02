package com.cinebook.controller.admin;

import com.cinebook.dto.PricingRuleCreateRequest;
import com.cinebook.dto.PricingRuleResponse;
import com.cinebook.dto.PricingRuleUpdateRequest;
import com.cinebook.service.PricingRuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminPricingController {

    private final PricingRuleService pricingRuleService;

    public AdminPricingController(PricingRuleService pricingRuleService) {
        this.pricingRuleService = pricingRuleService;
    }

    @PostMapping("/showtimes/{showtimeId}/pricing")
    public ResponseEntity<PricingRuleResponse> createPricingRule(@PathVariable Long showtimeId, @RequestBody PricingRuleCreateRequest request) {
        PricingRuleResponse response = pricingRuleService.createPricingRule(showtimeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/pricing/{id}")
    public ResponseEntity<PricingRuleResponse> updatePricingRule(@PathVariable Long id, @RequestBody PricingRuleUpdateRequest request) {
        PricingRuleResponse response = pricingRuleService.updatePricingRule(id, request);
        return ResponseEntity.ok(response);
    }
}
