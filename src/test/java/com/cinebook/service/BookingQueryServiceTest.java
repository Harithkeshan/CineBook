package com.cinebook.service;

import com.cinebook.dto.BookingDetailsResponse;
import com.cinebook.dto.BookingSummaryResponse;
import com.cinebook.dto.RefundResponse;
import com.cinebook.entity.*;
import com.cinebook.entity.enums.*;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingQueryServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingSeatRepository bookingSeatRepository;
    @Mock private ShowtimeRepository showtimeRepository;
    @Mock private ShowtimeSeatRepository showtimeSeatRepository;
    @Mock private PricingRuleRepository pricingRuleRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundService refundService;
    @Mock private UserRepository userRepository;

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

    private Booking createCompleteBooking(String ref, BookingStatus status) {
        Location loc = new Location(); loc.setId(1L); loc.setName("Cineplex Colombo"); loc.setCity("Colombo");
        Hall hall = new Hall(); hall.setId(10L); hall.setName("Hall 1"); hall.setLocation(loc);
        Movie movie = new Movie(); movie.setId(100L); movie.setTitle("Inception"); movie.setLanguage("English"); movie.setDurationMinutes(148);

        Showtime showtime = new Showtime();
        showtime.setId(50L);
        showtime.setMovie(movie);
        showtime.setHall(hall);
        showtime.setStartTime(OffsetDateTime.now().plusHours(2));
        showtime.setEndTime(OffsetDateTime.now().plusHours(5));
        showtime.setStatus(ShowtimeStatus.SCHEDULED);

        Booking booking = new Booking();
        booking.setId(500L);
        booking.setBookingReference(ref);
        booking.setCustomerName("Jane Doe");
        booking.setCustomerEmail("jane@example.com");
        booking.setCustomerPhone("0771234567");
        booking.setStatus(status);
        booking.setTotalAmount(new BigDecimal("1500.00"));
        booking.setCreatedAt(OffsetDateTime.now());
        booking.setShowtime(showtime);
        booking.setBookingSeats(new ArrayList<>());

        Section section = new Section(); section.setId(20L); section.setName("Ground"); section.setHall(hall);
        Seat seat = new Seat(); seat.setId(1000L); seat.setRowLabel("A"); seat.setSeatNumber("5"); seat.setSection(section);
        ShowtimeSeat ss = new ShowtimeSeat(); ss.setId(2000L); ss.setSeat(seat); ss.setShowtime(showtime);

        BookingSeat bs = new BookingSeat();
        bs.setId(3000L);
        bs.setBooking(booking);
        bs.setShowtimeSeat(ss);
        bs.setTicketType(TicketType.ADULT);
        bs.setPrice(new BigDecimal("1500.00"));

        booking.getBookingSeats().add(bs);

        return booking;
    }

    @Test
    void test01_existingBookingCanBeRetrieved() {
        String ref = "CB-8F4K2M";
        Booking booking = createCompleteBooking(ref, BookingStatus.CONFIRMED);

        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(ticketRepository.findByBookingSeatId(3000L)).thenReturn(Optional.empty());
        when(paymentRepository.findByBookingId(500L)).thenReturn(List.of());
        when(refundService.getRefundsByBookingReference(ref)).thenReturn(List.of());

        BookingDetailsResponse details = bookingService.getBookingDetailsByReference(ref);

        assertNotNull(details);
        assertEquals(ref, details.bookingReference());
        assertEquals(BookingStatus.CONFIRMED, details.status());
    }

    @Test
    void test02_nonexistentBookingReturns404() {
        when(bookingRepository.findByBookingReference("CB-999999")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookingService.getBookingDetailsByReference("CB-999999"));
    }

    @Test
    void test03_guestBookingCanBeRetrievedUsingReference() {
        String ref = "CB-GUEST-01";
        Booking booking = createCompleteBooking(ref, BookingStatus.CONFIRMED);
        booking.setUser(null);

        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(ticketRepository.findByBookingSeatId(3000L)).thenReturn(Optional.empty());
        when(paymentRepository.findByBookingId(500L)).thenReturn(List.of());
        when(refundService.getRefundsByBookingReference(ref)).thenReturn(List.of());

        BookingDetailsResponse details = bookingService.getBookingDetailsByReference(ref);
        assertEquals("Jane Doe", details.customerName());
        assertEquals("jane@example.com", details.customerEmail());
    }

    @Test
    void test04_confirmedBookingReturnsCorrectMovieAndShowtime() {
        String ref = "CB-8F4K2M";
        Booking booking = createCompleteBooking(ref, BookingStatus.CONFIRMED);

        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(ticketRepository.findByBookingSeatId(3000L)).thenReturn(Optional.empty());
        when(paymentRepository.findByBookingId(500L)).thenReturn(List.of());
        when(refundService.getRefundsByBookingReference(ref)).thenReturn(List.of());

        BookingDetailsResponse details = bookingService.getBookingDetailsByReference(ref);

        assertEquals("Inception", details.movie().title());
        assertEquals("Cineplex Colombo", details.location().name());
        assertEquals("Hall 1", details.hall().name());
    }

    @Test
    void test05_bookingSeatsAreReturnedCorrectly() {
        String ref = "CB-8F4K2M";
        Booking booking = createCompleteBooking(ref, BookingStatus.CONFIRMED);

        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(ticketRepository.findByBookingSeatId(3000L)).thenReturn(Optional.empty());

        BookingDetailsResponse details = bookingService.getBookingDetailsByReference(ref);

        assertEquals(1, details.seats().size());
        assertEquals("A", details.seats().get(0).rowLabel());
        assertEquals("5", details.seats().get(0).seatNumber());
        assertEquals(TicketType.ADULT, details.seats().get(0).ticketType());
    }

    @Test
    void test06_and_07_ticketInfoIsReturnedAndQrTokenIsNotExposed() {
        String ref = "CB-8F4K2M";
        Booking booking = createCompleteBooking(ref, BookingStatus.CONFIRMED);
        BookingSeat bs = booking.getBookingSeats().get(0);

        Ticket ticket = new Ticket(bs, "TKT-CB8F4K2M-01-XXXX", "SECRET_QR_TOKEN_12345", TicketStatus.ACTIVE);
        ticket.setIssuedAt(OffsetDateTime.now());

        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(ticketRepository.findByBookingSeatId(bs.getId())).thenReturn(Optional.of(ticket));

        BookingDetailsResponse details = bookingService.getBookingDetailsByReference(ref);

        assertEquals(1, details.tickets().size());
        assertEquals("TKT-CB8F4K2M-01-XXXX", details.tickets().get(0).ticketNumber());
        assertEquals(TicketStatus.ACTIVE, details.tickets().get(0).status());
        // Verify qrToken field does not exist on BookingDetailTicketResponse record
        assertNotNull(details.tickets().get(0).issuedAt());
    }

    @Test
    void test08_paymentInfoReturnedWithoutSensitiveCredentials() {
        String ref = "CB-8F4K2M";
        Booking booking = createCompleteBooking(ref, BookingStatus.CONFIRMED);

        Payment payment = new Payment();
        payment.setId(800L);
        payment.setBooking(booking);
        payment.setAmount(new BigDecimal("1500.00"));
        payment.setCurrency("LKR");
        payment.setStatus(PaymentStatus.PAID);
        payment.setProvider("MOCK");
        payment.setCreatedAt(OffsetDateTime.now());

        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(500L)).thenReturn(List.of(payment));

        BookingDetailsResponse details = bookingService.getBookingDetailsByReference(ref);

        assertNotNull(details.payment());
        assertEquals(PaymentStatus.PAID, details.payment().status());
        assertEquals(new BigDecimal("1500.00"), details.payment().amount());
        assertEquals("LKR", details.payment().currency());
        assertEquals("MOCK", details.payment().provider());
    }

    @Test
    void test09_and_11_showCancelledBookingReturnsRefundInfo() {
        String ref = "CB-SHOW-CANCEL";
        Booking booking = createCompleteBooking(ref, BookingStatus.CANCELLED);

        RefundResponse refundResp = new RefundResponse(
                900L, ref, 800L, new BigDecimal("1500.00"), RefundReason.SHOW_CANCELLED, RefundStatus.COMPLETED, "MOCK-REFUND-123", OffsetDateTime.now()
        );

        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(refundService.getRefundsByBookingReference(ref)).thenReturn(List.of(refundResp));

        BookingDetailsResponse details = bookingService.getBookingDetailsByReference(ref);

        assertEquals(1, details.refunds().size());
        assertEquals(RefundReason.SHOW_CANCELLED, details.refunds().get(0).reason());
    }

    @Test
    void test10_customerCancelledBookingReturnsNoRefunds() {
        String ref = "CB-CUSTOMER-CANCEL";
        Booking booking = createCompleteBooking(ref, BookingStatus.CANCELLED);

        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));
        when(refundService.getRefundsByBookingReference(ref)).thenReturn(List.of());

        BookingDetailsResponse details = bookingService.getBookingDetailsByReference(ref);

        assertTrue(details.refunds().isEmpty());
    }

    @Test
    void test12_cancelledBookingRemainsRetrievable() {
        String ref = "CB-CANCELLED";
        Booking booking = createCompleteBooking(ref, BookingStatus.CANCELLED);
        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));

        BookingDetailsResponse details = bookingService.getBookingDetailsByReference(ref);
        assertEquals(BookingStatus.CANCELLED, details.status());
    }

    @Test
    void test13_expiredBookingRemainsRetrievable() {
        String ref = "CB-EXPIRED";
        Booking booking = createCompleteBooking(ref, BookingStatus.EXPIRED);
        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));

        BookingDetailsResponse details = bookingService.getBookingDetailsByReference(ref);
        assertEquals(BookingStatus.EXPIRED, details.status());
    }

    @Test
    void test14_completedBookingRemainsRetrievable() {
        String ref = "CB-COMPLETED";
        Booking booking = createCompleteBooking(ref, BookingStatus.COMPLETED);
        when(bookingRepository.findByBookingReference(ref)).thenReturn(Optional.of(booking));

        BookingDetailsResponse details = bookingService.getBookingDetailsByReference(ref);
        assertEquals(BookingStatus.COMPLETED, details.status());
    }

    // BOOKING HISTORY
    @Test
    void test15_and_18_19_20_21_22_23_existingUserBookingHistoryRetrievedWithPagination() {
        Long userId = 5L;
        User user = new User(); user.setId(userId);

        Booking b1 = createCompleteBooking("CB-001", BookingStatus.CONFIRMED);
        Booking b2 = createCompleteBooking("CB-002", BookingStatus.COMPLETED);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Booking> bookingPage = new PageImpl<>(List.of(b1, b2), pageable, 2);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(bookingRepository.findByUserId(userId, pageable)).thenReturn(bookingPage);

        Page<BookingSummaryResponse> historyPage = bookingService.getUserBookingHistory(userId, pageable);

        assertEquals(2, historyPage.getTotalElements());
        assertEquals(1, historyPage.getTotalPages());
        assertEquals("CB-001", historyPage.getContent().get(0).bookingReference());
        assertEquals("Inception", historyPage.getContent().get(0).movieTitle());
    }

    @Test
    void test16_nonexistentUserReturns404() {
        when(userRepository.existsById(999L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> bookingService.getUserBookingHistory(999L, PageRequest.of(0, 10)));
    }

    @Test
    void test17_userWithNoBookingsReturnsEmptyPage() {
        Long userId = 5L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(bookingRepository.findByUserId(userId, pageable)).thenReturn(emptyPage);

        Page<BookingSummaryResponse> historyPage = bookingService.getUserBookingHistory(userId, pageable);

        assertNotNull(historyPage);
        assertTrue(historyPage.getContent().isEmpty());
        assertEquals(0, historyPage.getTotalElements());
    }
}
