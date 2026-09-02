package com.cinebook.service;

import com.cinebook.dto.CancelBookingResponse;
import com.cinebook.entity.*;
import com.cinebook.entity.enums.BookingStatus;
import com.cinebook.entity.enums.ShowtimeSeatStatus;
import com.cinebook.entity.enums.TicketStatus;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCancellationTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private ShowtimeSeatRepository showtimeSeatRepository;

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundService refundService;

    @Mock
    private UserRepository userRepository;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository,
                bookingSeatRepository,
                showtimeRepository,
                showtimeSeatRepository,
                pricingRuleRepository,
                ticketRepository,
                paymentRepository,
                refundService,
                userRepository
        );
    }

    private Booking createBooking(String ref, BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference(ref);
        booking.setStatus(status);
        booking.setBookingSeats(new ArrayList<>());
        return booking;
    }

    private BookingSeat addBookingSeat(Booking booking, ShowtimeSeatStatus seatStatus) {
        ShowtimeSeat ss = new ShowtimeSeat();
        ss.setId(100L);
        ss.setStatus(seatStatus);

        BookingSeat bs = new BookingSeat();
        bs.setId(10L);
        bs.setBooking(booking);
        bs.setShowtimeSeat(ss);

        booking.getBookingSeats().add(bs);
        return bs;
    }

    @Test
    void test1_PendingBookingCanBeCancelled() {
        String ref = "CB-8F4K2M";
        Booking booking = createBooking(ref, BookingStatus.PENDING);
        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CancelBookingResponse response = bookingService.cancelBookingByCustomer(ref);

        assertEquals(BookingStatus.CANCELLED, response.status());
        assertFalse(response.refundIssued());
        assertTrue(response.message().contains("No refund"));
    }

    @Test
    void test2_ConfirmedBookingCanBeCancelled() {
        String ref = "CB-8F4K2M";
        Booking booking = createBooking(ref, BookingStatus.CONFIRMED);
        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CancelBookingResponse response = bookingService.cancelBookingByCustomer(ref);

        assertEquals(BookingStatus.CANCELLED, response.status());
        assertFalse(response.refundIssued());
    }

    @Test
    void test3_CancelledBookingCannotBeCancelledAgain() {
        String ref = "CB-8F4K2M";
        Booking booking = createBooking(ref, BookingStatus.CANCELLED);
        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, () -> bookingService.cancelBookingByCustomer(ref));
    }

    @Test
    void test4_ExpiredBookingCannotBeCancelled() {
        String ref = "CB-8F4K2M";
        Booking booking = createBooking(ref, BookingStatus.EXPIRED);
        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, () -> bookingService.cancelBookingByCustomer(ref));
    }

    @Test
    void test5_CompletedBookingCannotBeCancelled() {
        String ref = "CB-8F4K2M";
        Booking booking = createBooking(ref, BookingStatus.COMPLETED);
        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, () -> bookingService.cancelBookingByCustomer(ref));
    }

    @Test
    void test6_CustomerCancellationCreatesNoRefund() {
        String ref = "CB-8F4K2M";
        Booking booking = createBooking(ref, BookingStatus.CONFIRMED);
        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CancelBookingResponse response = bookingService.cancelBookingByCustomer(ref);

        assertFalse(response.refundIssued());
    }

    @Test
    void test7_CustomerCancellationReleasesBookedSeatsToAvailable() {
        String ref = "CB-8F4K2M";
        Booking booking = createBooking(ref, BookingStatus.CONFIRMED);
        BookingSeat bs = addBookingSeat(booking, ShowtimeSeatStatus.BOOKED);

        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(showtimeSeatRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bookingService.cancelBookingByCustomer(ref);

        assertEquals(ShowtimeSeatStatus.AVAILABLE, bs.getShowtimeSeat().getStatus());
        assertNull(bs.getShowtimeSeat().getHoldExpiresAt());
    }

    @Test
    void test8_CustomerCancellationChangesActiveTicketsToCancelled() {
        String ref = "CB-8F4K2M";
        Booking booking = createBooking(ref, BookingStatus.CONFIRMED);
        BookingSeat bs = addBookingSeat(booking, ShowtimeSeatStatus.BOOKED);

        Ticket ticket = new Ticket(bs, "TKT-123", "QR-123", TicketStatus.ACTIVE);

        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(ticketRepository.findByBookingSeatId(bs.getId())).thenReturn(Optional.of(ticket));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bookingService.cancelBookingByCustomer(ref);

        assertEquals(TicketStatus.CANCELLED, ticket.getStatus());
    }

    @Test
    void test9_NonexistentBookingCancellationReturns404() {
        when(bookingRepository.findByBookingReference("CB-999999")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookingService.cancelBookingByCustomer("CB-999999"));
    }
}
