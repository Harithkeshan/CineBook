package com.cinebook.service;

import com.cinebook.dto.PricingRuleCreateRequest;
import com.cinebook.dto.PricingRuleResponse;
import com.cinebook.dto.PricingRuleUpdateRequest;
import com.cinebook.entity.PricingRule;
import com.cinebook.entity.Section;
import com.cinebook.entity.Showtime;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.PricingRuleRepository;
import com.cinebook.repository.SectionRepository;
import com.cinebook.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PricingRuleService {

    private final PricingRuleRepository pricingRuleRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SectionRepository sectionRepository;

    public PricingRuleService(
            PricingRuleRepository pricingRuleRepository,
            ShowtimeRepository showtimeRepository,
            SectionRepository sectionRepository
    ) {
        this.pricingRuleRepository = pricingRuleRepository;
        this.showtimeRepository = showtimeRepository;
        this.sectionRepository = sectionRepository;
    }

    public List<PricingRule> getPricingRulesByShowtimeId(Long showtimeId) {
        return pricingRuleRepository.findByShowtimeId(showtimeId);
    }

    public PricingRule getPricingRuleById(Long id) {
        return pricingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing rule not found with id: " + id));
    }

    @Transactional
    public PricingRuleResponse createPricingRule(Long showtimeId, PricingRuleCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Pricing rule request must not be null");
        }
        if (request.sectionId() == null) {
            throw new IllegalArgumentException("sectionId is required");
        }
        if (request.ticketType() == null) {
            throw new IllegalArgumentException("ticketType is required");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + showtimeId));

        Section section = sectionRepository.findById(request.sectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + request.sectionId()));

        if (!section.getHall().getId().equals(showtime.getHall().getId())) {
            throw new IllegalArgumentException("Section " + request.sectionId() + " does not belong to the same hall as showtime " + showtimeId);
        }

        if (pricingRuleRepository.existsByShowtimeIdAndSectionIdAndTicketType(showtimeId, request.sectionId(), request.ticketType())) {
            throw new IllegalStateException("Pricing rule already exists for showtime " + showtimeId + ", section " + request.sectionId() + ", and ticket type " + request.ticketType());
        }

        PricingRule rule = new PricingRule();
        rule.setShowtime(showtime);
        rule.setSection(section);
        rule.setTicketType(request.ticketType());
        rule.setPrice(request.price());

        rule = pricingRuleRepository.save(rule);
        return mapToResponse(rule);
    }

    @Transactional
    public PricingRuleResponse updatePricingRule(Long id, PricingRuleUpdateRequest request) {
        PricingRule rule = getPricingRuleById(id);

        if (request != null && request.price() != null) {
            if (request.price().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Price must be greater than 0");
            }
            rule.setPrice(request.price());
        }

        rule = pricingRuleRepository.save(rule);
        return mapToResponse(rule);
    }

    private PricingRuleResponse mapToResponse(PricingRule rule) {
        return new PricingRuleResponse(
                rule.getId(),
                rule.getShowtime() != null ? rule.getShowtime().getId() : null,
                rule.getSection() != null ? rule.getSection().getId() : null,
                rule.getTicketType(),
                rule.getPrice()
        );
    }
}
