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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentProvider paymentProvider;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                bookingRepository,
                paymentProvider,
                "LKR"
        );
    }

    private Booking createBooking(String reference, BookingStatus status, BigDecimal totalAmount) {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference(reference);
        booking.setStatus(status);
        booking.setTotalAmount(totalAmount);
        return booking;
    }

    private Payment createPayment(Long id, Booking booking, PaymentStatus status, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setBooking(booking);
        payment.setProvider("MOCK");
        payment.setProviderTransactionId("MOCK-TXN-123");
        payment.setAmount(amount);
        payment.setCurrency("LKR");
        payment.setStatus(status);
        return payment;
    }

    @Test
    void test1_InitiatePaymentForValidPendingBooking() {
        String ref = "CB-8F4K2M";
        Booking booking = createBooking(ref, BookingStatus.PENDING, new BigDecimal("3200.00"));

        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdAndStatus(1L, PaymentStatus.PROCESSING)).thenReturn(List.of());
        when(paymentProvider.getProviderName()).thenReturn("MOCK");
        when(paymentProvider.initiatePayment(eq(booking), eq(new BigDecimal("3200.00")), eq("LKR")))
                .thenReturn(new PaymentProvider.InitiateResult("MOCK-TXN-999", "http://redirect.url"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.initiatePayment(ref, new InitiatePaymentRequest(null));

        assertNotNull(response);
        assertEquals(ref, response.bookingReference());
        assertEquals("MOCK", response.provider());
        assertEquals("MOCK-TXN-999", response.providerTransactionId());
        assertEquals(new BigDecimal("3200.00"), response.amount());
        assertEquals("LKR", response.currency());
        assertEquals(PaymentStatus.PROCESSING, response.status());
    }

    @Test
    void test2_PaymentAmountEqualsBookingTotalAmount() {
        String ref = "CB-8F4K2M";
        BigDecimal expectedAmount = new BigDecimal("3200.00");
        Booking booking = createBooking(ref, BookingStatus.PENDING, expectedAmount);

        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdAndStatus(1L, PaymentStatus.PROCESSING)).thenReturn(List.of());
        when(paymentProvider.getProviderName()).thenReturn("MOCK");
        when(paymentProvider.initiatePayment(eq(booking), eq(expectedAmount), eq("LKR")))
                .thenReturn(new PaymentProvider.InitiateResult("MOCK-TXN-111", "http://url"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.initiatePayment(ref, null);

        assertEquals(expectedAmount, response.amount());
    }

    @Test
    void test3_SuccessfulPaymentConfirmationUpdatesStatuses() {
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.PENDING, new BigDecimal("3200.00"));
        Payment payment = createPayment(15L, booking, PaymentStatus.PROCESSING, new BigDecimal("3200.00"));

        when(paymentRepository.findById(15L)).thenReturn(Optional.of(payment));
        when(paymentProvider.verifyPayment(eq(payment), eq(true)))
                .thenReturn(new PaymentProvider.VerifyResult(true, "MOCK-TXN-123", "Success"));

        PaymentResponse response = paymentService.confirmPayment(15L, new ConfirmPaymentRequest(true));

        assertEquals(PaymentStatus.PAID, response.status());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(paymentRepository).save(payment);
        verify(bookingRepository).save(booking);
    }

    @Test
    void test4_FailedPaymentConfirmationLeavesBookingPending() {
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.PENDING, new BigDecimal("3200.00"));
        Payment payment = createPayment(15L, booking, PaymentStatus.PROCESSING, new BigDecimal("3200.00"));

        when(paymentRepository.findById(15L)).thenReturn(Optional.of(payment));
        when(paymentProvider.verifyPayment(eq(payment), eq(false)))
                .thenReturn(new PaymentProvider.VerifyResult(false, "MOCK-TXN-123", "Declined"));

        PaymentResponse response = paymentService.confirmPayment(15L, new ConfirmPaymentRequest(false));

        assertEquals(PaymentStatus.FAILED, response.status());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        verify(paymentRepository).save(payment);
    }

    @Test
    void test5_CannotPayConfirmedBooking() {
        String ref = "CB-8F4K2M";
        Booking booking = createBooking(ref, BookingStatus.CONFIRMED, new BigDecimal("3200.00"));
        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, () -> paymentService.initiatePayment(ref, null));
    }

    @Test
    void test6_CannotPayCancelledBooking() {
        String ref = "CB-8F4K2M";
        Booking booking = createBooking(ref, BookingStatus.CANCELLED, new BigDecimal("3200.00"));
        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, () -> paymentService.initiatePayment(ref, null));
    }

    @Test
    void test7_CannotPayExpiredBooking() {
        String ref = "CB-8F4K2M";
        Booking booking = createBooking(ref, BookingStatus.PENDING, new BigDecimal("3200.00"));
        booking.setExpiresAt(OffsetDateTime.now().minusMinutes(5));

        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, () -> paymentService.initiatePayment(ref, null));
    }

    @Test
    void test8_CannotConfirmAlreadyPaidPayment() {
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.CONFIRMED, new BigDecimal("3200.00"));
        Payment payment = createPayment(15L, booking, PaymentStatus.PAID, new BigDecimal("3200.00"));

        when(paymentRepository.findById(15L)).thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class, () -> paymentService.confirmPayment(15L, new ConfirmPaymentRequest(true)));
    }

    @Test
    void test9_CannotConfirmPaymentWithMismatchedAmount() {
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.PENDING, new BigDecimal("3200.00"));
        Payment payment = createPayment(15L, booking, PaymentStatus.PROCESSING, new BigDecimal("1000.00"));

        when(paymentRepository.findById(15L)).thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class, () -> paymentService.confirmPayment(15L, new ConfirmPaymentRequest(true)));
    }

    @Test
    void test10_NonexistentBookingReturns404() {
        when(bookingRepository.findByBookingReference("CB-999999")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> paymentService.initiatePayment("CB-999999", null));
    }

    @Test
    void test11_NonexistentPaymentReturns404() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> paymentService.confirmPayment(999L, null));
    }

    @Test
    void test12_MultiplePaymentAttemptsAllowedWhenPreviousFailed() {
        String ref = "CB-8F4K2M";
        Booking booking = createBooking(ref, BookingStatus.PENDING, new BigDecimal("3200.00"));

        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdAndStatus(1L, PaymentStatus.PROCESSING)).thenReturn(List.of());
        when(paymentProvider.getProviderName()).thenReturn("MOCK");
        when(paymentProvider.initiatePayment(eq(booking), any(), any()))
                .thenReturn(new PaymentProvider.InitiateResult("MOCK-TXN-RETRY", "http://redirect"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.initiatePayment(ref, null);
        assertNotNull(response);
        assertEquals(PaymentStatus.PROCESSING, response.status());
    }
}
