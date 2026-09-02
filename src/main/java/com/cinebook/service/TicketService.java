package com.cinebook.service;

import com.cinebook.dto.TicketCheckInResponse;
import com.cinebook.dto.TicketResponse;
import com.cinebook.dto.TicketVerificationResponse;
import com.cinebook.entity.Booking;
import com.cinebook.entity.BookingSeat;
import com.cinebook.entity.Seat;
import com.cinebook.entity.Section;
import com.cinebook.entity.Showtime;
import com.cinebook.entity.Ticket;
import com.cinebook.entity.enums.BookingStatus;
import com.cinebook.entity.enums.ShowtimeStatus;
import com.cinebook.entity.enums.TicketStatus;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TicketService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;

    public TicketService(TicketRepository ticketRepository, BookingRepository bookingRepository) {
        this.ticketRepository = ticketRepository;
        this.bookingRepository = bookingRepository;
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
    }

    public Ticket getTicketByNumber(String ticketNumber) {
        return ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with number: " + ticketNumber));
    }

    public Ticket getTicketByQrToken(String qrToken) {
        return ticketRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with QR token: " + qrToken));
    }

    public Ticket getTicketByBookingSeatId(Long bookingSeatId) {
        return ticketRepository.findByBookingSeatId(bookingSeatId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found for booking seat id: " + bookingSeatId));
    }

    @Transactional
    public List<Ticket> generateTicketsForBooking(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking must not be null");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot generate tickets for booking with status: " + booking.getStatus());
        }

        List<BookingSeat> bookingSeats = booking.getBookingSeats();
        if (bookingSeats == null || bookingSeats.isEmpty()) {
            throw new IllegalStateException("Booking has no seats to generate tickets for");
        }

        List<Ticket> generatedTickets = new ArrayList<>();
        int index = 1;

        for (BookingSeat bs : bookingSeats) {
            Optional<Ticket> existingOpt = ticketRepository.findByBookingSeatId(bs.getId());
            if (existingOpt.isPresent()) {
                generatedTickets.add(existingOpt.get());
            } else {
                String ticketNumber = generateUniqueTicketNumber(booking.getBookingReference(), index++);
                String qrToken = generateUniqueQrToken();

                Ticket ticket = new Ticket();
                ticket.setBookingSeat(bs);
                ticket.setTicketNumber(ticketNumber);
                ticket.setQrToken(qrToken);
                ticket.setStatus(TicketStatus.ACTIVE);
                ticket.setIssuedAt(OffsetDateTime.now());
                ticket.setUsedAt(null);

                ticket = ticketRepository.save(ticket);
                generatedTickets.add(ticket);
            }
        }

        return generatedTickets;
    }

    public List<TicketResponse> getTicketsByBookingReference(String bookingReference) {
        if (!bookingRepository.existsByBookingReference(bookingReference)) {
            throw new ResourceNotFoundException("Booking not found with reference: " + bookingReference);
        }

        List<Ticket> tickets = ticketRepository.findByBookingSeatBookingBookingReference(bookingReference);
        return tickets.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public TicketResponse getTicketByNumberDto(String ticketNumber) {
        Ticket ticket = getTicketByNumber(ticketNumber);
        return mapToResponse(ticket);
    }

    public TicketVerificationResponse verifyTicketByQrToken(String qrToken) {
        Ticket ticket = ticketRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with QR token: " + qrToken));

        boolean valid = (ticket.getStatus() == TicketStatus.ACTIVE);
        BookingSeat bs = ticket.getBookingSeat();
        Seat seat = bs != null && bs.getShowtimeSeat() != null ? bs.getShowtimeSeat().getSeat() : null;

        String seatStr = seat != null ? (seat.getRowLabel() + seat.getSeatNumber()) : "N/A";
        String bookingRef = bs != null && bs.getBooking() != null ? bs.getBooking().getBookingReference() : null;

        return new TicketVerificationResponse(
                valid,
                ticket.getTicketNumber(),
                ticket.getStatus(),
                bookingRef,
                seatStr,
                bs != null ? bs.getTicketType() : null
        );
    }

    @Transactional
    public TicketCheckInResponse checkInTicket(String qrToken) {
        Ticket ticket = ticketRepository.findByQrTokenWithLock(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with QR token: " + qrToken));

        BookingSeat bs = ticket.getBookingSeat();
        Showtime showtime = bs != null && bs.getShowtimeSeat() != null ? bs.getShowtimeSeat().getShowtime() : null;
        if (showtime != null && showtime.getStatus() == ShowtimeStatus.CANCELLED) {
            throw new IllegalStateException("Showtime is cancelled.");
        }

        if (ticket.getStatus() == TicketStatus.USED) {
            throw new IllegalStateException("Ticket has already been used.");
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Ticket is cancelled.");
        }
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new IllegalStateException("Ticket cannot be checked in from status: " + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(OffsetDateTime.now());
        ticket = ticketRepository.save(ticket);

        Seat seat = bs != null && bs.getShowtimeSeat() != null ? bs.getShowtimeSeat().getSeat() : null;
        String seatStr = seat != null ? (seat.getRowLabel() + seat.getSeatNumber()) : "N/A";
        String bookingRef = bs != null && bs.getBooking() != null ? bs.getBooking().getBookingReference() : null;

        return new TicketCheckInResponse(
                true,
                ticket.getTicketNumber(),
                bookingRef,
                ticket.getStatus(),
                seatStr,
                bs != null ? bs.getTicketType() : null,
                ticket.getUsedAt()
        );
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        BookingSeat bs = ticket.getBookingSeat();
        Seat seat = bs != null && bs.getShowtimeSeat() != null ? bs.getShowtimeSeat().getSeat() : null;
        Section section = seat != null ? seat.getSection() : null;

        return new TicketResponse(
                ticket.getTicketNumber(),
                bs != null && bs.getBooking() != null ? bs.getBooking().getBookingReference() : null,
                seat != null ? seat.getId() : null,
                section != null ? section.getName() : null,
                seat != null ? seat.getRowLabel() : null,
                seat != null ? seat.getSeatNumber() : null,
                bs != null ? bs.getTicketType() : null,
                bs != null ? bs.getPrice() : null,
                ticket.getStatus(),
                ticket.getIssuedAt(),
                ticket.getQrToken()
        );
    }

    private String generateUniqueTicketNumber(String bookingRef, int seatIndex) {
        String number;
        String cleanRef = bookingRef != null ? bookingRef.replace("CB-", "") : "REF";
        do {
            number = "TKT-" + cleanRef + "-" + String.format("%02d", seatIndex) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        } while (ticketRepository.existsByTicketNumber(number));
        return number;
    }

    private String generateUniqueQrToken() {
        String token;
        do {
            byte[] randomBytes = new byte[24];
            SECURE_RANDOM.nextBytes(randomBytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        } while (ticketRepository.existsByQrToken(token));
        return token;
    }
}
