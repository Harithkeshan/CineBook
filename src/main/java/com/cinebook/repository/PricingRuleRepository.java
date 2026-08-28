package com.cinebook.repository;

import com.cinebook.entity.PricingRule;
import com.cinebook.entity.enums.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    List<PricingRule> findByShowtimeId(Long showtimeId);
    Optional<PricingRule> findByShowtimeIdAndSectionIdAndTicketType(Long showtimeId, Long sectionId, TicketType ticketType);
}
