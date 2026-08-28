package com.cinebook.repository;

import com.cinebook.entity.Payment;
import com.cinebook.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingId(Long bookingId);
    List<Payment> findByBookingIdAndStatus(Long bookingId, PaymentStatus status);
    Optional<Payment> findByProviderTransactionId(String providerTransactionId);
}
