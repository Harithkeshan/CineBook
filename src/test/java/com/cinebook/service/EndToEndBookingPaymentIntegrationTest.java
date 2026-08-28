package com.cinebook.service;

import com.cinebook.dto.*;
import com.cinebook.entity.*;
import com.cinebook.entity.enums.*;
import com.cinebook.payment.MockPaymentProvider;
import com.cinebook.payment.PaymentProvider;
import com.cinebook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndToEndBookingPaymentIntegrationTest {

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private ShowtimeSeatRepository showtimeSeatRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private ShowtimeSeatService showtimeSeatService;
    private BookingService bookingService;
    private PaymentService paymentService;
    private PaymentProvider mockPaymentProvider;

    @BeforeEach
    void setUp() {
        mockPaymentProvider = new MockPaymentProvider();
        showtimeSeatService = new ShowtimeSeatService(showtimeRepository, showtimeSeatRepository, 5);
        bookingService = new BookingService(
                bookingRepository,
                bookingSeatRepository,
                showtimeRepository,
                showtimeSeatRepository,
                pricingRuleRepository
        );
        paymentService = new PaymentService(
                paymentRepository,
                bookingRepository,
                mockPaymentProvider,
                "LKR"
        );
    }

    @Test
    void testFullWorkflow_HoldSeat_CreateBooking_InitiatePayment_ConfirmPayment() {
        Long showtimeId = 1L;
        Long seatId = 10L;
        Long sectionId = 1L;

        Showtime showtime = new Showtime();
        showtime.setId(showtimeId);
        showtime.setStatus(ShowtimeStatus.SCHEDULED);

        Section section = new Section();
        section.setId(sectionId);
        section.setName("Ground");

        Seat seat = new Seat();
        seat.setId(seatId);
        seat.setSection(section);
        seat.setRowLabel("A");
        seat.setSeatNumber("1");

        ShowtimeSeat ss = new ShowtimeSeat();
        ss.setId(100L);
        ss.setSeat(seat);
        ss.setShowtime(showtime);
        ss.setStatus(ShowtimeSeatStatus.AVAILABLE);

        PricingRule rule = new PricingRule();
        rule.setTicketType(TicketType.ADULT);
        rule.setPrice(new BigDecimal("1200.00"));

        // Step 1: Hold seat
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(seatId)))
                .thenReturn(List.of(ss));

        HoldSeatsRequest holdRequest = new HoldSeatsRequest(List.of(seatId));
        SeatHoldResponse holdResponse = showtimeSeatService.holdSeats(showtimeId, holdRequest);

        assertNotNull(holdResponse);
        assertEquals(ShowtimeSeatStatus.HELD, ss.getStatus());

        // Step 2: Create guest booking
        when(pricingRuleRepository.findByShowtimeIdAndSectionIdAndTicketType(showtimeId, sectionId, TicketType.ADULT))
                .thenReturn(Optional.of(rule));
        when(bookingRepository.existsByBookingReference(any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(50L);
            return b;
        });

        BookingRequest bookingRequest = new BookingRequest(
                "John Doe",
                "john@example.com",
                "0771234567",
                List.of(new BookingSeatRequest(seatId, TicketType.ADULT))
        );

        BookingResponse bookingResponse = bookingService.createGuestBooking(showtimeId, bookingRequest);

        assertNotNull(bookingResponse);
        assertEquals(BookingStatus.PENDING, bookingResponse.status());
        assertEquals(new BigDecimal("1200.00"), bookingResponse.totalAmount());
        assertEquals(ShowtimeSeatStatus.BOOKED, ss.getStatus());

        // Step 3: Initiate Payment
        Booking savedBooking = new Booking();
        savedBooking.setId(50L);
        savedBooking.setBookingReference(bookingResponse.bookingReference());
        savedBooking.setStatus(BookingStatus.PENDING);
        savedBooking.setTotalAmount(new BigDecimal("1200.00"));

        when(bookingRepository.findByBookingReference(bookingResponse.bookingReference()))
                .thenReturn(Optional.of(savedBooking));
        when(paymentRepository.findByBookingIdAndStatus(50L, PaymentStatus.PROCESSING))
                .thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(15L);
            return p;
        });

        PaymentResponse initPaymentResponse = paymentService.initiatePayment(bookingResponse.bookingReference(), null);

        assertNotNull(initPaymentResponse);
        assertEquals(PaymentStatus.PROCESSING, initPaymentResponse.status());
        assertEquals(new BigDecimal("1200.00"), initPaymentResponse.amount());
        assertEquals("MOCK", initPaymentResponse.provider());

        // Step 4: Confirm Payment
        Payment processingPayment = new Payment();
        processingPayment.setId(15L);
        processingPayment.setBooking(savedBooking);
        processingPayment.setProvider("MOCK");
        processingPayment.setProviderTransactionId(initPaymentResponse.providerTransactionId());
        processingPayment.setAmount(new BigDecimal("1200.00"));
        processingPayment.setCurrency("LKR");
        processingPayment.setStatus(PaymentStatus.PROCESSING);

        when(paymentRepository.findById(15L)).thenReturn(Optional.of(processingPayment));

        PaymentResponse confirmPaymentResponse = paymentService.confirmPayment(15L, new ConfirmPaymentRequest(true));

        assertNotNull(confirmPaymentResponse);
        assertEquals(PaymentStatus.PAID, confirmPaymentResponse.status());
        assertEquals(BookingStatus.CONFIRMED, savedBooking.getStatus());

        // Step 5: Verify seats remain BOOKED
        assertEquals(ShowtimeSeatStatus.BOOKED, ss.getStatus(), "Seats must remain BOOKED after payment confirmation");
    }
}
