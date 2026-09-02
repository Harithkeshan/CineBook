package com.cinebook.service;

import com.cinebook.dto.TicketCheckInResponse;
import com.cinebook.dto.TicketResponse;
import com.cinebook.dto.TicketVerificationResponse;
import com.cinebook.entity.*;
import com.cinebook.entity.enums.BookingStatus;
import com.cinebook.entity.enums.ShowtimeStatus;
import com.cinebook.entity.enums.TicketStatus;
import com.cinebook.entity.enums.TicketType;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private BookingRepository bookingRepository;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, bookingRepository);
    }

    private Booking createBooking(String ref, BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference(ref);
        booking.setStatus(status);
        booking.setBookingSeats(new ArrayList<>());
        return booking;
    }

    private BookingSeat createBookingSeat(Long id, Booking booking, TicketType ticketType, BigDecimal price, String seatNum) {
        Showtime showtime = new Showtime();
        showtime.setId(1000L);
        showtime.setStatus(ShowtimeStatus.SCHEDULED);

        Section section = new Section();
        section.setId(1L);
        section.setName("Ground");

        Seat seat = new Seat();
        seat.setId(10L);
        seat.setSection(section);
        seat.setRowLabel("A");
        seat.setSeatNumber(seatNum);

        ShowtimeSeat ss = new ShowtimeSeat();
        ss.setId(100L);
        ss.setShowtime(showtime);
        ss.setSeat(seat);

        BookingSeat bs = new BookingSeat();
        bs.setId(id);
        bs.setBooking(booking);
        bs.setShowtimeSeat(ss);
        bs.setTicketType(ticketType);
        bs.setPrice(price);

        booking.getBookingSeats().add(bs);
        return bs;
    }

    @Test
    void test1_ConfirmedBookingGeneratesOneTicketPerBookingSeat() {
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.CONFIRMED);
        BookingSeat bs1 = createBookingSeat(10L, booking, TicketType.ADULT, new BigDecimal("1200.00"), "1");
        BookingSeat bs2 = createBookingSeat(11L, booking, TicketType.CHILD, new BigDecimal("800.00"), "2");

        when(ticketRepository.findByBookingSeatId(any())).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Ticket> tickets = ticketService.generateTicketsForBooking(booking);

        assertEquals(2, tickets.size());
        assertEquals(TicketStatus.ACTIVE, tickets.get(0).getStatus());
        assertNull(tickets.get(0).getUsedAt());
        assertNotNull(tickets.get(0).getIssuedAt());
        assertTrue(tickets.get(0).getTicketNumber().startsWith("TKT-"));
        assertNotNull(tickets.get(0).getQrToken());
    }

    @Test
    void test2_PendingBookingCannotGenerateTickets() {
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.PENDING);
        assertThrows(IllegalStateException.class, () -> ticketService.generateTicketsForBooking(booking));
    }

    @Test
    void test3_CancelledBookingCannotGenerateTickets() {
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.CANCELLED);
        assertThrows(IllegalStateException.class, () -> ticketService.generateTicketsForBooking(booking));
    }

    @Test
    void test4_IdempotentTicketGenerationDoesNotCreateDuplicates() {
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.CONFIRMED);
        BookingSeat bs = createBookingSeat(10L, booking, TicketType.ADULT, new BigDecimal("1200.00"), "1");

        Ticket existingTicket = new Ticket();
        existingTicket.setId(55L);
        existingTicket.setBookingSeat(bs);
        existingTicket.setTicketNumber("TKT-CB8F4K2M-01-XXXX");
        existingTicket.setQrToken("EXISTING-TOKEN");
        existingTicket.setStatus(TicketStatus.ACTIVE);

        when(ticketRepository.findByBookingSeatId(10L)).thenReturn(Optional.of(existingTicket));

        List<Ticket> tickets = ticketService.generateTicketsForBooking(booking);

        assertEquals(1, tickets.size());
        assertEquals("TKT-CB8F4K2M-01-XXXX", tickets.get(0).getTicketNumber());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void test5_GetTicketsByBookingReferenceReturnsAllTickets() {
        String ref = "CB-8F4K2M";
        Booking booking = createBooking(ref, BookingStatus.CONFIRMED);
        BookingSeat bs = createBookingSeat(10L, booking, TicketType.ADULT, new BigDecimal("1200.00"), "1");

        Ticket t1 = new Ticket(bs, "TKT-CB8F4K2M-01-AAA", "QR-AAA", TicketStatus.ACTIVE);
        t1.setIssuedAt(OffsetDateTime.now());

        when(bookingRepository.existsByBookingReference(ref)).thenReturn(true);
        when(ticketRepository.findByBookingSeatBookingBookingReference(ref)).thenReturn(List.of(t1));

        List<TicketResponse> responses = ticketService.getTicketsByBookingReference(ref);

        assertEquals(1, responses.size());
        assertEquals("TKT-CB8F4K2M-01-AAA", responses.get(0).ticketNumber());
        assertEquals(ref, responses.get(0).bookingReference());
    }

    @Test
    void test6_GetNonexistentBookingTicketsReturns404() {
        when(bookingRepository.existsByBookingReference("CB-999999")).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> ticketService.getTicketsByBookingReference("CB-999999"));
    }

    @Test
    void test7_VerifyTicketByQrTokenReturnsCorrectVerificationResponse() {
        String qrToken = "SECURE-QR-123";
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.CONFIRMED);
        BookingSeat bs = createBookingSeat(10L, booking, TicketType.ADULT, new BigDecimal("1200.00"), "1");

        Ticket ticket = new Ticket(bs, "TKT-CB8F4K2M-01-AAA", qrToken, TicketStatus.ACTIVE);

        when(ticketRepository.findByQrToken(qrToken)).thenReturn(Optional.of(ticket));

        TicketVerificationResponse response = ticketService.verifyTicketByQrToken(qrToken);

        assertTrue(response.valid());
        assertEquals("TKT-CB8F4K2M-01-AAA", response.ticketNumber());
        assertEquals(TicketStatus.ACTIVE, response.status());
        assertEquals("CB-8F4K2M", response.bookingReference());
        assertEquals("A1", response.seat());
        assertEquals(TicketType.ADULT, response.ticketType());
    }

    @Test
    void test8_InvalidQrTokenReturns404() {
        when(ticketRepository.findByQrToken("INVALID-TOKEN")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> ticketService.verifyTicketByQrToken("INVALID-TOKEN"));
    }

    @Test
    void test9_ActiveTicketCheckInTransitionsToUsedAndSetsUsedAt() {
        String qrToken = "SECURE-QR-999";
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.CONFIRMED);
        BookingSeat bs = createBookingSeat(10L, booking, TicketType.ADULT, new BigDecimal("1200.00"), "1");

        Ticket ticket = new Ticket(bs, "TKT-CB8F4K2M-01-AAA", qrToken, TicketStatus.ACTIVE);
        OffsetDateTime issuedAt = OffsetDateTime.now().minusHours(1);
        ticket.setIssuedAt(issuedAt);

        when(ticketRepository.findByQrTokenWithLock(qrToken)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketCheckInResponse response = ticketService.checkInTicket(qrToken);

        assertTrue(response.valid());
        assertEquals(TicketStatus.USED, response.status());
        assertNotNull(response.usedAt());
        assertEquals(issuedAt, ticket.getIssuedAt(), "issuedAt must remain unchanged");
        assertEquals("TKT-CB8F4K2M-01-AAA", ticket.getTicketNumber(), "ticketNumber must remain unchanged");
        assertEquals(qrToken, ticket.getQrToken(), "qrToken must remain unchanged");
    }

    @Test
    void test10_SecondCheckInAttemptThrowsConflictIllegalStateException() {
        String qrToken = "SECURE-QR-999";
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.CONFIRMED);
        BookingSeat bs = createBookingSeat(10L, booking, TicketType.ADULT, new BigDecimal("1200.00"), "1");

        Ticket ticket = new Ticket(bs, "TKT-CB8F4K2M-01-AAA", qrToken, TicketStatus.USED);
        OffsetDateTime originalUsedAt = OffsetDateTime.now().minusMinutes(30);
        ticket.setUsedAt(originalUsedAt);

        when(ticketRepository.findByQrTokenWithLock(qrToken)).thenReturn(Optional.of(ticket));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ticketService.checkInTicket(qrToken));
        assertEquals("Ticket has already been used.", ex.getMessage());
        assertEquals(originalUsedAt, ticket.getUsedAt(), "usedAt must not be modified on second scan");
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void test11_CancelledTicketCheckInThrowsConflictIllegalStateException() {
        String qrToken = "SECURE-QR-999";
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.CONFIRMED);
        BookingSeat bs = createBookingSeat(10L, booking, TicketType.ADULT, new BigDecimal("1200.00"), "1");

        Ticket ticket = new Ticket(bs, "TKT-CB8F4K2M-01-AAA", qrToken, TicketStatus.CANCELLED);

        when(ticketRepository.findByQrTokenWithLock(qrToken)).thenReturn(Optional.of(ticket));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ticketService.checkInTicket(qrToken));
        assertEquals("Ticket is cancelled.", ex.getMessage());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void test12_CancelledShowtimeTicketCheckInThrowsConflictIllegalStateException() {
        String qrToken = "SECURE-QR-999";
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.CONFIRMED);
        BookingSeat bs = createBookingSeat(10L, booking, TicketType.ADULT, new BigDecimal("1200.00"), "1");
        bs.getShowtimeSeat().getShowtime().setStatus(ShowtimeStatus.CANCELLED);

        Ticket ticket = new Ticket(bs, "TKT-CB8F4K2M-01-AAA", qrToken, TicketStatus.ACTIVE);

        when(ticketRepository.findByQrTokenWithLock(qrToken)).thenReturn(Optional.of(ticket));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ticketService.checkInTicket(qrToken));
        assertEquals("Showtime is cancelled.", ex.getMessage());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void test13_VerifyEndpointDoesNotConsumeActiveTicket() {
        String qrToken = "SECURE-QR-123";
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.CONFIRMED);
        BookingSeat bs = createBookingSeat(10L, booking, TicketType.ADULT, new BigDecimal("1200.00"), "1");

        Ticket ticket = new Ticket(bs, "TKT-CB8F4K2M-01-AAA", qrToken, TicketStatus.ACTIVE);

        when(ticketRepository.findByQrToken(qrToken)).thenReturn(Optional.of(ticket));

        TicketVerificationResponse response = ticketService.verifyTicketByQrToken(qrToken);

        assertTrue(response.valid());
        assertEquals(TicketStatus.ACTIVE, ticket.getStatus(), "Verify endpoint must NOT change status to USED");
        assertNull(ticket.getUsedAt(), "Verify endpoint must NOT set usedAt");
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void test14_VerifyEndpointReportsUsedTicketAsInvalid() {
        String qrToken = "SECURE-QR-123";
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.CONFIRMED);
        BookingSeat bs = createBookingSeat(10L, booking, TicketType.ADULT, new BigDecimal("1200.00"), "1");

        Ticket ticket = new Ticket(bs, "TKT-CB8F4K2M-01-AAA", qrToken, TicketStatus.USED);

        when(ticketRepository.findByQrToken(qrToken)).thenReturn(Optional.of(ticket));

        TicketVerificationResponse response = ticketService.verifyTicketByQrToken(qrToken);

        assertFalse(response.valid());
        assertEquals(TicketStatus.USED, response.status());
    }

    @Test
    void test15_VerifyEndpointReportsCancelledTicketAsInvalid() {
        String qrToken = "SECURE-QR-123";
        Booking booking = createBooking("CB-8F4K2M", BookingStatus.CONFIRMED);
        BookingSeat bs = createBookingSeat(10L, booking, TicketType.ADULT, new BigDecimal("1200.00"), "1");

        Ticket ticket = new Ticket(bs, "TKT-CB8F4K2M-01-AAA", qrToken, TicketStatus.CANCELLED);

        when(ticketRepository.findByQrToken(qrToken)).thenReturn(Optional.of(ticket));

        TicketVerificationResponse response = ticketService.verifyTicketByQrToken(qrToken);

        assertFalse(response.valid());
        assertEquals(TicketStatus.CANCELLED, response.status());
    }
}
