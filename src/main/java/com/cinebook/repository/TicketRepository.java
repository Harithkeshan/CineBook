package com.cinebook.repository;

import com.cinebook.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketNumber(String ticketNumber);
    Optional<Ticket> findByQrToken(String qrToken);
    Optional<Ticket> findByBookingSeatId(Long bookingSeatId);
}
