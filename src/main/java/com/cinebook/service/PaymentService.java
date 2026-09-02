package com.cinebook.service;

import com.cinebook.dto.ConfirmPaymentRequest;
import com.cinebook.dto.InitiatePaymentRequest;
import com.cinebook.dto.PaymentResponse;
import com.cinebook.entity.Booking;
import com.cinebook.entity.Payment;
import com.cinebook.entity.enums.BookingStatus;
import com.cinebook.entity.enums.PaymentStatus;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.payment.PaymentProvider;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentProvider paymentProvider;
    private final TicketService ticketService;
    private final String defaultCurrency;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            PaymentProvider paymentProvider,
            TicketService ticketService,
            @Value("${cinebook.payment.currency:LKR}") String defaultCurrency
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.paymentProvider = paymentProvider;
        this.ticketService = ticketService;
        this.defaultCurrency = defaultCurrency;
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    public List<Payment> getPaymentsByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    @Transactional
    public PaymentResponse initiatePayment(String bookingReference, InitiatePaymentRequest request) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + bookingReference));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking is already confirmed");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Cannot initiate payment for booking with status: " + booking.getStatus());
        }
        if (booking.getExpiresAt() != null && booking.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Booking has expired");
        }

        List<Payment> processingPayments = paymentRepository.findByBookingIdAndStatus(booking.getId(), PaymentStatus.PROCESSING);
        if (!processingPayments.isEmpty()) {
            throw new IllegalStateException("A payment is already processing for this booking");
        }

        BigDecimal amount = booking.getTotalAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invalid booking total amount");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setProvider(paymentProvider.getProviderName());
        payment.setAmount(amount);
        payment.setCurrency(defaultCurrency);
        payment.setStatus(PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        PaymentProvider.InitiateResult initiateResult = paymentProvider.initiatePayment(booking, amount, defaultCurrency);
        payment.setProviderTransactionId(initiateResult.transactionId());
        payment.setStatus(PaymentStatus.PROCESSING);
        payment = paymentRepository.save(payment);

        return mapToResponse(payment);
    }

    @Transactional
    public PaymentResponse confirmPayment(Long paymentId, ConfirmPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        Booking booking = payment.getBooking();

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Payment is already completed");
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking is already confirmed");
        }
        if (payment.getStatus() != PaymentStatus.PROCESSING && payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment cannot be confirmed from status: " + payment.getStatus());
        }
        if (payment.getAmount().compareTo(booking.getTotalAmount()) != 0) {
            throw new IllegalStateException("Payment amount does not match booking total amount");
        }

        boolean simulateSuccess = (request == null || request.success() == null) ? true : request.success();
        PaymentProvider.VerifyResult verifyResult = paymentProvider.verifyPayment(payment, simulateSuccess);

        if (verifyResult.isSuccess()) {
            payment.setStatus(PaymentStatus.PAID);
            booking.setStatus(BookingStatus.CONFIRMED);
            paymentRepository.save(payment);
            bookingRepository.save(booking);

            ticketService.generateTicketsForBooking(booking);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }

        return mapToResponse(payment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking() != null ? payment.getBooking().getBookingReference() : null,
                payment.getProvider(),
                payment.getProviderTransactionId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}
