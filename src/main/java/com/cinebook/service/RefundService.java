package com.cinebook.service;

import com.cinebook.dto.RefundResponse;
import com.cinebook.entity.Booking;
import com.cinebook.entity.Payment;
import com.cinebook.entity.Refund;
import com.cinebook.entity.enums.PaymentStatus;
import com.cinebook.entity.enums.RefundReason;
import com.cinebook.entity.enums.RefundStatus;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.PaymentRepository;
import com.cinebook.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public RefundService(
            RefundRepository refundRepository,
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository
    ) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    public Refund getRefundById(Long id) {
        return refundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found with id: " + id));
    }

    public List<Refund> getRefundsByPaymentId(Long paymentId) {
        return refundRepository.findByPaymentId(paymentId);
    }

    @Transactional
    public Refund processShowCancellationRefund(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment must not be null");
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Cannot refund payment with status: " + payment.getStatus());
        }

        List<Refund> existingRefunds = refundRepository.findByPaymentId(payment.getId());
        if (!existingRefunds.isEmpty()) {
            return existingRefunds.get(0);
        }

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(payment.getAmount());
        refund.setReason(RefundReason.SHOW_CANCELLED);
        refund.setStatus(RefundStatus.COMPLETED);
        refund.setProviderRefundId("MOCK-REFUND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        refund = refundRepository.save(refund);

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        return refund;
    }

    public List<RefundResponse> getRefundsByBookingReference(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + bookingReference));

        List<Payment> payments = paymentRepository.findByBookingId(booking.getId());
        List<RefundResponse> responses = new ArrayList<>();

        for (Payment payment : payments) {
            List<Refund> refunds = refundRepository.findByPaymentId(payment.getId());
            for (Refund refund : refunds) {
                responses.add(mapToResponse(refund, bookingReference));
            }
        }

        return responses;
    }

    public RefundResponse getRefundResponseById(Long refundId) {
        Refund refund = getRefundById(refundId);
        String bookingRef = refund.getPayment() != null && refund.getPayment().getBooking() != null
                ? refund.getPayment().getBooking().getBookingReference()
                : null;
        return mapToResponse(refund, bookingRef);
    }

    private RefundResponse mapToResponse(Refund refund, String bookingReference) {
        return new RefundResponse(
                refund.getId(),
                bookingReference,
                refund.getPayment() != null ? refund.getPayment().getId() : null,
                refund.getAmount(),
                refund.getReason(),
                refund.getStatus(),
                refund.getProviderRefundId(),
                refund.getCreatedAt()
        );
    }
}
