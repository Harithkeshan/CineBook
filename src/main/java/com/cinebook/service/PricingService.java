package com.cinebook.service;

import com.cinebook.entity.PricingRule;
import com.cinebook.entity.enums.TicketType;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.PricingRuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PricingService {

    private final PricingRuleRepository pricingRuleRepository;

    public PricingService(PricingRuleRepository pricingRuleRepository) {
        this.pricingRuleRepository = pricingRuleRepository;
    }

    public List<PricingRule> getPricingRulesByShowtimeId(Long showtimeId) {
        return pricingRuleRepository.findByShowtimeId(showtimeId);
    }

    public PricingRule getPricingRule(Long showtimeId, Long sectionId, TicketType ticketType) {
        return pricingRuleRepository.findByShowtimeIdAndSectionIdAndTicketType(showtimeId, sectionId, ticketType)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing rule not found for showtime: " + showtimeId +
                        ", section: " + sectionId + ", ticketType: " + ticketType));
    }
}
