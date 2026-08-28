package com.cinebook.repository;

import com.cinebook.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String bookingReference);
    boolean existsByBookingReference(String bookingReference);
    List<Booking> findByUserId(Long userId);
    Page<Booking> findByUserId(Long userId, Pageable pageable);
    List<Booking> findByShowtimeId(Long showtimeId);
}
