package com.cinebook.repository;

import com.cinebook.entity.Ticket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketNumber(String ticketNumber);
    Optional<Ticket> findByQrToken(String qrToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.qrToken = :qrToken")
    Optional<Ticket> findByQrTokenWithLock(@Param("qrToken") String qrToken);

    Optional<Ticket> findByBookingSeatId(Long bookingSeatId);
    boolean existsByTicketNumber(String ticketNumber);
    boolean existsByQrToken(String qrToken);
    List<Ticket> findByBookingSeatBookingBookingReference(String bookingReference);
}
