package com.cinebook.service;

import com.cinebook.dto.BookingRequest;
import com.cinebook.dto.BookingResponse;
import com.cinebook.dto.BookingSeatRequest;
import com.cinebook.entity.Booking;
import com.cinebook.entity.PricingRule;
import com.cinebook.entity.Seat;
import com.cinebook.entity.Section;
import com.cinebook.entity.Showtime;
import com.cinebook.entity.ShowtimeSeat;
import com.cinebook.entity.enums.BookingStatus;
import com.cinebook.entity.enums.ShowtimeSeatStatus;
import com.cinebook.entity.enums.ShowtimeStatus;
import com.cinebook.entity.enums.TicketType;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.exception.SeatUnavailableException;
import com.cinebook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

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
    private com.cinebook.repository.TicketRepository ticketRepository;

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

    private Showtime createShowtime(Long id, ShowtimeStatus status) {
        Showtime showtime = new Showtime();
        showtime.setId(id);
        showtime.setStatus(status);
        return showtime;
    }

    private ShowtimeSeat createShowtimeSeat(Long id, Long seatId, Long sectionId, String sectionName, ShowtimeSeatStatus status, OffsetDateTime holdExpiresAt) {
        Section section = new Section();
        section.setId(sectionId);
        section.setName(sectionName);

        Seat seat = new Seat();
        seat.setId(seatId);
        seat.setSection(section);
        seat.setRowLabel("A");
        seat.setSeatNumber("1");

        ShowtimeSeat ss = new ShowtimeSeat();
        ss.setId(id);
        ss.setSeat(seat);
        ss.setStatus(status);
        ss.setHoldExpiresAt(holdExpiresAt);
        return ss;
    }

    private PricingRule createPricingRule(Long showtimeId, Long sectionId, TicketType ticketType, BigDecimal price) {
        PricingRule rule = new PricingRule();
        rule.setTicketType(ticketType);
        rule.setPrice(price);
        return rule;
    }

    @Test
    void test1_SuccessfulGuestBookingOneAdultSeat() {
        Long showtimeId = 1L;
        Long seatId = 10L;
        Long sectionId = 1L;

        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);
        ShowtimeSeat ss = createShowtimeSeat(100L, seatId, sectionId, "Ground", ShowtimeSeatStatus.HELD, OffsetDateTime.now().plusMinutes(5));
        PricingRule rule = createPricingRule(showtimeId, sectionId, TicketType.ADULT, new BigDecimal("1200.00"));

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(seatId))).thenReturn(List.of(ss));
        when(pricingRuleRepository.findByShowtimeIdAndSectionIdAndTicketType(showtimeId, sectionId, TicketType.ADULT)).thenReturn(Optional.of(rule));
        when(bookingRepository.existsByBookingReference(any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRequest request = new BookingRequest(
                "John Doe",
                "john@example.com",
                "0771234567",
                List.of(new BookingSeatRequest(seatId, TicketType.ADULT))
        );

        BookingResponse response = bookingService.createGuestBooking(showtimeId, request);

        assertNotNull(response);
        assertTrue(response.bookingReference().startsWith("CB-"));
        assertEquals("John Doe", response.customerName());
        assertEquals("john@example.com", response.customerEmail());
        assertEquals("0771234567", response.customerPhone());
        assertEquals(BookingStatus.PENDING, response.status());
        assertEquals(new BigDecimal("1200.00"), response.totalAmount());
        assertEquals(1, response.seats().size());
        assertEquals(ShowtimeSeatStatus.BOOKED, ss.getStatus());
        assertNull(ss.getHoldExpiresAt());
    }

    @Test
    void test2_SuccessfulGuestBookingMultipleSeats() {
        Long showtimeId = 1L;
        Long seatId1 = 10L;
        Long seatId2 = 11L;
        Long sectionId = 1L;

        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);
        ShowtimeSeat ss1 = createShowtimeSeat(100L, seatId1, sectionId, "Ground", ShowtimeSeatStatus.HELD, OffsetDateTime.now().plusMinutes(5));
        ShowtimeSeat ss2 = createShowtimeSeat(101L, seatId2, sectionId, "Ground", ShowtimeSeatStatus.HELD, OffsetDateTime.now().plusMinutes(5));
        PricingRule rule = createPricingRule(showtimeId, sectionId, TicketType.ADULT, new BigDecimal("1200.00"));

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(seatId1, seatId2))).thenReturn(List.of(ss1, ss2));
        when(pricingRuleRepository.findByShowtimeIdAndSectionIdAndTicketType(showtimeId, sectionId, TicketType.ADULT)).thenReturn(Optional.of(rule));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRequest request = new BookingRequest(
                "Jane Doe",
                "jane@example.com",
                "0777654321",
                List.of(new BookingSeatRequest(seatId1, TicketType.ADULT), new BookingSeatRequest(seatId2, TicketType.ADULT))
        );

        BookingResponse response = bookingService.createGuestBooking(showtimeId, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("2400.00"), response.totalAmount());
        assertEquals(2, response.seats().size());
        assertEquals(ShowtimeSeatStatus.BOOKED, ss1.getStatus());
        assertEquals(ShowtimeSeatStatus.BOOKED, ss2.getStatus());
    }

    @Test
    void test3_SuccessfulBookingAdultAndChildTickets() {
        Long showtimeId = 1L;
        Long seatId1 = 10L;
        Long seatId2 = 11L;
        Long sectionId = 1L;

        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);
        ShowtimeSeat ss1 = createShowtimeSeat(100L, seatId1, sectionId, "Ground", ShowtimeSeatStatus.HELD, OffsetDateTime.now().plusMinutes(5));
        ShowtimeSeat ss2 = createShowtimeSeat(101L, seatId2, sectionId, "Ground", ShowtimeSeatStatus.HELD, OffsetDateTime.now().plusMinutes(5));
        PricingRule adultRule = createPricingRule(showtimeId, sectionId, TicketType.ADULT, new BigDecimal("1200.00"));
        PricingRule childRule = createPricingRule(showtimeId, sectionId, TicketType.CHILD, new BigDecimal("800.00"));

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(seatId1, seatId2))).thenReturn(List.of(ss1, ss2));
        when(pricingRuleRepository.findByShowtimeIdAndSectionIdAndTicketType(showtimeId, sectionId, TicketType.ADULT)).thenReturn(Optional.of(adultRule));
        when(pricingRuleRepository.findByShowtimeIdAndSectionIdAndTicketType(showtimeId, sectionId, TicketType.CHILD)).thenReturn(Optional.of(childRule));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRequest request = new BookingRequest(
                "Family Customer",
                "family@example.com",
                "0770000000",
                List.of(new BookingSeatRequest(seatId1, TicketType.ADULT), new BookingSeatRequest(seatId2, TicketType.CHILD))
        );

        BookingResponse response = bookingService.createGuestBooking(showtimeId, request);

        assertEquals(new BigDecimal("2000.00"), response.totalAmount());
    }

    @Test
    void test4_AvailableSeatCannotBeBooked() {
        Long showtimeId = 1L;
        Long seatId = 10L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);
        ShowtimeSeat ss = createShowtimeSeat(100L, seatId, 1L, "Ground", ShowtimeSeatStatus.AVAILABLE, null);

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(seatId))).thenReturn(List.of(ss));

        BookingRequest request = new BookingRequest("Test", "test@example.com", "0771234567", List.of(new BookingSeatRequest(seatId, TicketType.ADULT)));

        assertThrows(SeatUnavailableException.class, () -> bookingService.createGuestBooking(showtimeId, request));
    }

    @Test
    void test5_BookedSeatCannotBeBooked() {
        Long showtimeId = 1L;
        Long seatId = 10L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);
        ShowtimeSeat ss = createShowtimeSeat(100L, seatId, 1L, "Ground", ShowtimeSeatStatus.BOOKED, null);

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(seatId))).thenReturn(List.of(ss));

        BookingRequest request = new BookingRequest("Test", "test@example.com", "0771234567", List.of(new BookingSeatRequest(seatId, TicketType.ADULT)));

        assertThrows(SeatUnavailableException.class, () -> bookingService.createGuestBooking(showtimeId, request));
    }

    @Test
    void test6_ExpiredHeldSeatCannotBeBooked() {
        Long showtimeId = 1L;
        Long seatId = 10L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);
        ShowtimeSeat ss = createShowtimeSeat(100L, seatId, 1L, "Ground", ShowtimeSeatStatus.HELD, OffsetDateTime.now().minusMinutes(1));

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(seatId))).thenReturn(List.of(ss));

        BookingRequest request = new BookingRequest("Test", "test@example.com", "0771234567", List.of(new BookingSeatRequest(seatId, TicketType.ADULT)));

        assertThrows(SeatUnavailableException.class, () -> bookingService.createGuestBooking(showtimeId, request));
    }

    @Test
    void test7_DuplicateSeatIdsRejected() {
        Long showtimeId = 1L;
        BookingRequest request = new BookingRequest(
                "Test", "test@example.com", "0771234567",
                List.of(new BookingSeatRequest(10L, TicketType.ADULT), new BookingSeatRequest(10L, TicketType.CHILD))
        );

        assertThrows(IllegalArgumentException.class, () -> bookingService.createGuestBooking(showtimeId, request));
    }

    @Test
    void test8_NonexistentShowtimeRejected() {
        Long showtimeId = 999L;
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.empty());

        BookingRequest request = new BookingRequest("Test", "test@example.com", "0771234567", List.of(new BookingSeatRequest(10L, TicketType.ADULT)));

        assertThrows(ResourceNotFoundException.class, () -> bookingService.createGuestBooking(showtimeId, request));
    }

    @Test
    void test9_NonexistentSeatRejected() {
        Long showtimeId = 1L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(999L))).thenReturn(List.of());

        BookingRequest request = new BookingRequest("Test", "test@example.com", "0771234567", List.of(new BookingSeatRequest(999L, TicketType.ADULT)));

        assertThrows(ResourceNotFoundException.class, () -> bookingService.createGuestBooking(showtimeId, request));
    }

    @Test
    void test10_CancelledShowtimeRejected() {
        Long showtimeId = 1L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.CANCELLED);
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));

        BookingRequest request = new BookingRequest("Test", "test@example.com", "0771234567", List.of(new BookingSeatRequest(10L, TicketType.ADULT)));

        assertThrows(IllegalStateException.class, () -> bookingService.createGuestBooking(showtimeId, request));
    }

    @Test
    void test11_CompletedShowtimeRejected() {
        Long showtimeId = 1L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.COMPLETED);
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));

        BookingRequest request = new BookingRequest("Test", "test@example.com", "0771234567", List.of(new BookingSeatRequest(10L, TicketType.ADULT)));

        assertThrows(IllegalStateException.class, () -> bookingService.createGuestBooking(showtimeId, request));
    }

    @Test
    void test12_MissingCustomerNameRejected() {
        BookingRequest request = new BookingRequest("", "test@example.com", "0771234567", List.of(new BookingSeatRequest(10L, TicketType.ADULT)));
        assertThrows(IllegalArgumentException.class, () -> bookingService.createGuestBooking(1L, request));
    }

    @Test
    void test13_InvalidEmailRejected() {
        BookingRequest request = new BookingRequest("Test", "invalid-email", "0771234567", List.of(new BookingSeatRequest(10L, TicketType.ADULT)));
        assertThrows(IllegalArgumentException.class, () -> bookingService.createGuestBooking(1L, request));
    }

    @Test
    void test14_EmptySeatListRejected() {
        BookingRequest request = new BookingRequest("Test", "test@example.com", "0771234567", List.of());
        assertThrows(IllegalArgumentException.class, () -> bookingService.createGuestBooking(1L, request));
    }

    @Test
    void test15_AtomicityPartialFailureRejectsAllSeats() {
        Long showtimeId = 1L;
        Long seatId1 = 10L;
        Long seatId2 = 11L;

        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);
        ShowtimeSeat ss1 = createShowtimeSeat(100L, seatId1, 1L, "Ground", ShowtimeSeatStatus.HELD, OffsetDateTime.now().plusMinutes(5));
        ShowtimeSeat ss2 = createShowtimeSeat(101L, seatId2, 1L, "Ground", ShowtimeSeatStatus.BOOKED, null);

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(seatId1, seatId2))).thenReturn(List.of(ss1, ss2));

        BookingRequest request = new BookingRequest(
                "Test", "test@example.com", "0771234567",
                List.of(new BookingSeatRequest(seatId1, TicketType.ADULT), new BookingSeatRequest(seatId2, TicketType.ADULT))
        );

        assertThrows(SeatUnavailableException.class, () -> bookingService.createGuestBooking(showtimeId, request));
        assertEquals(ShowtimeSeatStatus.HELD, ss1.getStatus());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void test16_GuestBookingHasNullUserIdAndPendingStatus() {
        Long showtimeId = 1L;
        Long seatId = 10L;
        Long sectionId = 1L;

        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);
        ShowtimeSeat ss = createShowtimeSeat(100L, seatId, sectionId, "Ground", ShowtimeSeatStatus.HELD, OffsetDateTime.now().plusMinutes(5));
        PricingRule rule = createPricingRule(showtimeId, sectionId, TicketType.ADULT, new BigDecimal("1200.00"));

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(seatId))).thenReturn(List.of(ss));
        when(pricingRuleRepository.findByShowtimeIdAndSectionIdAndTicketType(showtimeId, sectionId, TicketType.ADULT)).thenReturn(Optional.of(rule));

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        when(bookingRepository.save(bookingCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        BookingRequest request = new BookingRequest(
                "Guest User",
                "guest@example.com",
                "0771234567",
                List.of(new BookingSeatRequest(seatId, TicketType.ADULT))
        );

        bookingService.createGuestBooking(showtimeId, request);

        Booking savedBooking = bookingCaptor.getValue();
        assertNull(savedBooking.getUser(), "Guest booking MUST have user = null");
        assertEquals(BookingStatus.PENDING, savedBooking.getStatus());
        assertEquals("Guest User", savedBooking.getCustomerName());
    }
}
