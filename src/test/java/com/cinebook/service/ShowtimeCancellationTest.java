package com.cinebook.service;

import com.cinebook.dto.CancelShowtimeResponse;
import com.cinebook.entity.*;
import com.cinebook.entity.enums.*;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowtimeCancellationTest {

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundService refundService;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ShowtimeSeatRepository showtimeSeatRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private HallRepository hallRepository;

    @Mock
    private SeatRepository seatRepository;

    private ShowtimeService showtimeService;

    @BeforeEach
    void setUp() {
        showtimeService = new ShowtimeService(
                showtimeRepository,
                bookingRepository,
                paymentRepository,
                refundService,
                ticketRepository,
                showtimeSeatRepository,
                movieRepository,
                hallRepository,
                seatRepository
        );
    }

    private Showtime createShowtime(Long id, ShowtimeStatus status) {
        Showtime showtime = new Showtime();
        showtime.setId(id);
        showtime.setStatus(status);
        return showtime;
    }

    private Booking createBooking(Long id, Showtime showtime, BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setShowtime(showtime);
        booking.setBookingReference("CB-REF-" + id);
        booking.setStatus(status);
        booking.setBookingSeats(new ArrayList<>());
        return booking;
    }

    private BookingSeat addBookingSeat(Booking booking, ShowtimeSeatStatus seatStatus) {
        ShowtimeSeat ss = new ShowtimeSeat();
        ss.setId(100L + booking.getId());
        ss.setStatus(seatStatus);

        BookingSeat bs = new BookingSeat();
        bs.setId(10L + booking.getId());
        bs.setBooking(booking);
        bs.setShowtimeSeat(ss);

        booking.getBookingSeats().add(bs);
        return bs;
    }

    @Test
    void test10_ScheduledShowtimeCanBeCancelled() {
        Long showtimeId = 1L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(bookingRepository.findByShowtimeId(showtimeId)).thenReturn(List.of());

        CancelShowtimeResponse response = showtimeService.cancelShowtimeByCinema(showtimeId);

        assertEquals(ShowtimeStatus.CANCELLED, response.status());
        assertEquals(0, response.totalBookingsCancelled());
        assertEquals(0, response.totalRefundsProcessed());
    }

    @Test
    void test11_CancelledShowtimeCannotBeCancelledAgain() {
        Long showtimeId = 1L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.CANCELLED);
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));

        assertThrows(IllegalStateException.class, () -> showtimeService.cancelShowtimeByCinema(showtimeId));
    }

    @Test
    void test12_CompletedShowtimeCannotBeCancelled() {
        Long showtimeId = 1L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.COMPLETED);
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));

        assertThrows(IllegalStateException.class, () -> showtimeService.cancelShowtimeByCinema(showtimeId));
    }

    @Test
    void test13_ConfirmedAndPendingBookingsAreCancelledAndPaidPaymentsRefunded() {
        Long showtimeId = 1L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);

        Booking confirmedBooking = createBooking(101L, showtime, BookingStatus.CONFIRMED);
        BookingSeat bs1 = addBookingSeat(confirmedBooking, ShowtimeSeatStatus.BOOKED);
        Ticket t1 = new Ticket(bs1, "TKT-101", "QR-101", TicketStatus.ACTIVE);

        Booking pendingBooking = createBooking(102L, showtime, BookingStatus.PENDING);
        BookingSeat bs2 = addBookingSeat(pendingBooking, ShowtimeSeatStatus.HELD);

        Payment paidPayment = new Payment();
        paidPayment.setId(500L);
        paidPayment.setBooking(confirmedBooking);
        paidPayment.setAmount(new BigDecimal("2400.00"));
        paidPayment.setStatus(PaymentStatus.PAID);

        Refund mockRefund = new Refund(paidPayment, new BigDecimal("2400.00"), RefundReason.SHOW_CANCELLED, RefundStatus.COMPLETED);

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(bookingRepository.findByShowtimeId(showtimeId)).thenReturn(List.of(confirmedBooking, pendingBooking));
        when(ticketRepository.findByBookingSeatId(bs1.getId())).thenReturn(Optional.of(t1));
        when(ticketRepository.findByBookingSeatId(bs2.getId())).thenReturn(Optional.empty());
        when(paymentRepository.findByBookingIdAndStatus(101L, PaymentStatus.PAID)).thenReturn(List.of(paidPayment));
        when(paymentRepository.findByBookingIdAndStatus(102L, PaymentStatus.PAID)).thenReturn(List.of());
        when(refundService.processShowCancellationRefund(paidPayment)).thenReturn(mockRefund);

        CancelShowtimeResponse response = showtimeService.cancelShowtimeByCinema(showtimeId);

        assertEquals(ShowtimeStatus.CANCELLED, response.status());
        assertEquals(2, response.totalBookingsCancelled());
        assertEquals(1, response.totalRefundsProcessed());
        assertEquals(new BigDecimal("2400.00"), response.totalRefundAmount());

        assertEquals(BookingStatus.CANCELLED, confirmedBooking.getStatus());
        assertEquals(BookingStatus.CANCELLED, pendingBooking.getStatus());
        assertEquals(ShowtimeSeatStatus.AVAILABLE, bs1.getShowtimeSeat().getStatus());
        assertEquals(ShowtimeSeatStatus.AVAILABLE, bs2.getShowtimeSeat().getStatus());
        assertEquals(TicketStatus.CANCELLED, t1.getStatus());
    }

    @Test
    void test14_UsedTicketIsNotReactivatedOrCancelled() {
        Long showtimeId = 1L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);

        Booking booking = createBooking(101L, showtime, BookingStatus.CONFIRMED);
        BookingSeat bs = addBookingSeat(booking, ShowtimeSeatStatus.BOOKED);
        Ticket usedTicket = new Ticket(bs, "TKT-USED", "QR-USED", TicketStatus.USED);

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(bookingRepository.findByShowtimeId(showtimeId)).thenReturn(List.of(booking));
        when(ticketRepository.findByBookingSeatId(bs.getId())).thenReturn(Optional.of(usedTicket));
        when(paymentRepository.findByBookingIdAndStatus(101L, PaymentStatus.PAID)).thenReturn(List.of());

        showtimeService.cancelShowtimeByCinema(showtimeId);

        assertEquals(TicketStatus.USED, usedTicket.getStatus(), "Used tickets must remain USED");
    }

    @Test
    void test15_NonexistentShowtimeCancelReturns404() {
        when(showtimeRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> showtimeService.cancelShowtimeByCinema(999L));
    }
}
