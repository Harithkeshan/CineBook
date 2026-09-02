package com.cinebook.service;

import com.cinebook.dto.BookingDetailsResponse;
import com.cinebook.dto.BookingRequest;
import com.cinebook.dto.BookingResponse;
import com.cinebook.dto.BookingSeatRequest;
import com.cinebook.dto.BookingSeatResponse;
import com.cinebook.dto.BookingSummaryResponse;
import com.cinebook.dto.CancelBookingResponse;
import com.cinebook.dto.RefundResponse;
import com.cinebook.entity.Booking;
import com.cinebook.entity.BookingSeat;
import com.cinebook.entity.Hall;
import com.cinebook.entity.Location;
import com.cinebook.entity.Movie;
import com.cinebook.entity.Payment;
import com.cinebook.entity.PricingRule;
import com.cinebook.entity.Showtime;
import com.cinebook.entity.ShowtimeSeat;
import com.cinebook.entity.Ticket;
import com.cinebook.entity.enums.BookingStatus;
import com.cinebook.entity.enums.PaymentStatus;
import com.cinebook.entity.enums.ShowtimeSeatStatus;
import com.cinebook.entity.enums.ShowtimeStatus;
import com.cinebook.entity.enums.TicketStatus;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.exception.SeatUnavailableException;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.BookingSeatRepository;
import com.cinebook.repository.PaymentRepository;
import com.cinebook.repository.PricingRuleRepository;
import com.cinebook.repository.ShowtimeRepository;
import com.cinebook.repository.ShowtimeSeatRepository;
import com.cinebook.repository.TicketRepository;
import com.cinebook.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final RefundService refundService;
    private final UserRepository userRepository;

    public BookingService(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            ShowtimeRepository showtimeRepository,
            ShowtimeSeatRepository showtimeSeatRepository,
            PricingRuleRepository pricingRuleRepository,
            TicketRepository ticketRepository,
            PaymentRepository paymentRepository,
            RefundService refundService,
            UserRepository userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.showtimeRepository = showtimeRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.pricingRuleRepository = pricingRuleRepository;
        this.ticketRepository = ticketRepository;
        this.paymentRepository = paymentRepository;
        this.refundService = refundService;
        this.userRepository = userRepository;
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    public Booking getBookingByReference(String bookingReference) {
        return bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + bookingReference));
    }

    public List<Booking> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    public Page<Booking> getBookingsByUserId(Long userId, Pageable pageable) {
        return bookingRepository.findByUserId(userId, pageable);
    }

    public List<Booking> getBookingsByShowtimeId(Long showtimeId) {
        return bookingRepository.findByShowtimeId(showtimeId);
    }

    public BookingDetailsResponse getBookingDetailsByReference(String bookingReference) {
        Booking booking = getBookingByReference(bookingReference);

        Showtime showtime = booking.getShowtime();
        Movie movie = showtime != null ? showtime.getMovie() : null;
        Hall hall = showtime != null ? showtime.getHall() : null;
        Location location = hall != null ? hall.getLocation() : null;

        BookingDetailsResponse.MovieSummary movieSummary = movie != null
                ? new BookingDetailsResponse.MovieSummary(
                movie.getId(),
                movie.getTitle(),
                movie.getDurationMinutes(),
                movie.getLanguage(),
                movie.getGenre(),
                movie.getPosterUrl(),
                movie.getAgeRating()
        )
                : null;

        BookingDetailsResponse.ShowtimeSummary showtimeSummary = showtime != null
                ? new BookingDetailsResponse.ShowtimeSummary(
                showtime.getId(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getStatus()
        )
                : null;

        BookingDetailsResponse.LocationSummary locationSummary = location != null
                ? new BookingDetailsResponse.LocationSummary(
                location.getId(),
                location.getName(),
                location.getCity()
        )
                : null;

        BookingDetailsResponse.HallSummary hallSummary = hall != null
                ? new BookingDetailsResponse.HallSummary(
                hall.getId(),
                hall.getName()
        )
                : null;

        List<BookingDetailsResponse.BookingDetailSeatResponse> seatResponses = new ArrayList<>();
        if (booking.getBookingSeats() != null) {
            for (BookingSeat bs : booking.getBookingSeats()) {
                ShowtimeSeat ss = bs.getShowtimeSeat();
                seatResponses.add(new BookingDetailsResponse.BookingDetailSeatResponse(
                        ss != null && ss.getSeat() != null ? ss.getSeat().getId() : null,
                        ss != null && ss.getSeat() != null && ss.getSeat().getSection() != null ? ss.getSeat().getSection().getName() : null,
                        ss != null && ss.getSeat() != null ? ss.getSeat().getRowLabel() : null,
                        ss != null && ss.getSeat() != null ? ss.getSeat().getSeatNumber() : null,
                        bs.getTicketType(),
                        bs.getPrice()
                ));
            }
        }

        List<BookingDetailsResponse.BookingDetailTicketResponse> ticketResponses = new ArrayList<>();
        if (booking.getBookingSeats() != null) {
            for (BookingSeat bs : booking.getBookingSeats()) {
                Optional<Ticket> ticketOpt = ticketRepository.findByBookingSeatId(bs.getId());
                if (ticketOpt.isPresent()) {
                    Ticket t = ticketOpt.get();
                    ticketResponses.add(new BookingDetailsResponse.BookingDetailTicketResponse(
                            t.getTicketNumber(),
                            t.getStatus(),
                            t.getIssuedAt(),
                            t.getUsedAt()
                    ));
                }
            }
        }

        List<Payment> payments = paymentRepository.findByBookingId(booking.getId());
        BookingDetailsResponse.PaymentSummary paymentSummary = null;
        if (!payments.isEmpty()) {
            Payment p = payments.stream()
                    .filter(pay -> pay.getStatus() == PaymentStatus.PAID || pay.getStatus() == PaymentStatus.REFUNDED)
                    .findFirst()
                    .orElse(payments.get(payments.size() - 1));

            paymentSummary = new BookingDetailsResponse.PaymentSummary(
                    p.getId(),
                    p.getStatus(),
                    p.getAmount(),
                    p.getCurrency(),
                    p.getProvider() != null ? p.getProvider() : "MOCK",
                    p.getCreatedAt()
            );
        }

        List<RefundResponse> refunds = refundService.getRefundsByBookingReference(bookingReference);

        return new BookingDetailsResponse(
                booking.getBookingReference(),
                booking.getStatus(),
                booking.getCustomerName(),
                booking.getCustomerEmail(),
                booking.getCustomerPhone(),
                booking.getTotalAmount(),
                booking.getCreatedAt(),
                movieSummary,
                showtimeSummary,
                locationSummary,
                hallSummary,
                seatResponses,
                ticketResponses,
                paymentSummary,
                refunds
        );
    }

    public Page<BookingSummaryResponse> getUserBookingHistory(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Page<Booking> page = bookingRepository.findByUserId(userId, pageable);

        return page.map(b -> new BookingSummaryResponse(
                b.getBookingReference(),
                b.getShowtime() != null && b.getShowtime().getMovie() != null ? b.getShowtime().getMovie().getTitle() : null,
                b.getShowtime() != null ? b.getShowtime().getStartTime() : null,
                b.getShowtime() != null && b.getShowtime().getHall() != null && b.getShowtime().getHall().getLocation() != null ? b.getShowtime().getHall().getLocation().getName() : null,
                b.getShowtime() != null && b.getShowtime().getHall() != null ? b.getShowtime().getHall().getName() : null,
                b.getStatus(),
                b.getTotalAmount(),
                b.getCreatedAt()
        ));
    }

    @Transactional
    public BookingResponse createGuestBooking(Long showtimeId, BookingRequest request) {
        validateRequest(request);

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + showtimeId));

        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot create booking for showtime with status: " + showtime.getStatus());
        }

        List<Long> seatIds = request.seats().stream()
                .map(BookingSeatRequest::seatId)
                .toList();

        List<ShowtimeSeat> lockedSeats = showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, seatIds);
        if (lockedSeats.size() != seatIds.size()) {
            throw new ResourceNotFoundException("One or more requested seats do not exist or do not belong to the showtime's hall");
        }

        OffsetDateTime now = OffsetDateTime.now();
        Map<Long, ShowtimeSeat> seatMap = lockedSeats.stream()
                .collect(Collectors.toMap(ss -> ss.getSeat().getId(), Function.identity()));

        for (BookingSeatRequest seatReq : request.seats()) {
            ShowtimeSeat ss = seatMap.get(seatReq.seatId());
            if (ss == null) {
                throw new ResourceNotFoundException("Seat not found with id: " + seatReq.seatId());
            }
            if (ss.getStatus() == ShowtimeSeatStatus.BOOKED) {
                throw new SeatUnavailableException("Seat " + ss.getSeat().getId() + " is already booked");
            }
            if (ss.getStatus() == ShowtimeSeatStatus.AVAILABLE) {
                throw new SeatUnavailableException("Seat " + ss.getSeat().getId() + " is available but must be held before booking");
            }
            if (ss.getStatus() == ShowtimeSeatStatus.HELD) {
                if (ss.getHoldExpiresAt() == null || !ss.getHoldExpiresAt().isAfter(now)) {
                    ss.setStatus(ShowtimeSeatStatus.AVAILABLE);
                    ss.setHoldExpiresAt(null);
                    showtimeSeatRepository.save(ss);
                    throw new SeatUnavailableException("One or more selected seat holds have expired.");
                }
            }
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<SeatPriceInfo> seatPriceInfos = new ArrayList<>();

        for (BookingSeatRequest seatReq : request.seats()) {
            ShowtimeSeat ss = seatMap.get(seatReq.seatId());
            Long sectionId = ss.getSeat().getSection().getId();

            PricingRule pricingRule = pricingRuleRepository
                    .findByShowtimeIdAndSectionIdAndTicketType(showtimeId, sectionId, seatReq.ticketType())
                    .orElseThrow(() -> new IllegalStateException("Pricing rule not found for section " + sectionId + " and ticket type " + seatReq.ticketType()));

            BigDecimal price = pricingRule.getPrice();
            totalAmount = totalAmount.add(price);
            seatPriceInfos.add(new SeatPriceInfo(ss, seatReq.ticketType(), price));
        }

        String bookingRef = generateUniqueBookingReference();

        Booking booking = new Booking();
        booking.setUser(null);
        booking.setShowtime(showtime);
        booking.setBookingReference(bookingRef);
        booking.setCustomerName(request.customerName().trim());
        booking.setCustomerEmail(request.customerEmail().trim());
        booking.setCustomerPhone(request.customerPhone().trim());
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(totalAmount);
        booking = bookingRepository.save(booking);

        List<BookingSeatResponse> seatResponses = new ArrayList<>();

        for (SeatPriceInfo info : seatPriceInfos) {
            BookingSeat bookingSeat = new BookingSeat();
            bookingSeat.setBooking(booking);
            bookingSeat.setShowtimeSeat(info.showtimeSeat());
            bookingSeat.setTicketType(info.ticketType());
            bookingSeat.setPrice(info.price());
            bookingSeatRepository.save(bookingSeat);

            ShowtimeSeat ss = info.showtimeSeat();
            ss.setStatus(ShowtimeSeatStatus.BOOKED);
            ss.setHoldExpiresAt(null);
            showtimeSeatRepository.save(ss);

            seatResponses.add(new BookingSeatResponse(
                    ss.getSeat().getId(),
                    ss.getSeat().getSection() != null ? ss.getSeat().getSection().getName() : null,
                    ss.getSeat().getRowLabel(),
                    ss.getSeat().getSeatNumber(),
                    info.ticketType(),
                    info.price()
            ));
        }

        return new BookingResponse(
                booking.getBookingReference(),
                showtimeId,
                booking.getCustomerName(),
                booking.getCustomerEmail(),
                booking.getCustomerPhone(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getCreatedAt(),
                seatResponses
        );
    }

    @Transactional
    public CancelBookingResponse cancelBookingByCustomer(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + bookingReference));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw new IllegalStateException("Cannot cancel an expired booking");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed booking");
        }
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel booking with status: " + booking.getStatus());
        }

        List<BookingSeat> bookingSeats = booking.getBookingSeats();
        if (bookingSeats != null) {
            for (BookingSeat bs : bookingSeats) {
                ShowtimeSeat ss = bs.getShowtimeSeat();
                if (ss != null && (ss.getStatus() == ShowtimeSeatStatus.BOOKED || ss.getStatus() == ShowtimeSeatStatus.HELD)) {
                    ss.setStatus(ShowtimeSeatStatus.AVAILABLE);
                    ss.setHoldExpiresAt(null);
                    showtimeSeatRepository.save(ss);
                }

                Optional<Ticket> ticketOpt = ticketRepository.findByBookingSeatId(bs.getId());
                if (ticketOpt.isPresent()) {
                    Ticket ticket = ticketOpt.get();
                    if (ticket.getStatus() == TicketStatus.ACTIVE) {
                        ticket.setStatus(TicketStatus.CANCELLED);
                        ticketRepository.save(ticket);
                    }
                }
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        return new CancelBookingResponse(
                booking.getBookingReference(),
                BookingStatus.CANCELLED,
                false,
                "Booking cancelled. No refund is available for customer-initiated cancellation."
        );
    }

    private void validateRequest(BookingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Booking request body must not be null");
        }
        if (request.customerName() == null || request.customerName().isBlank()) {
            throw new IllegalArgumentException("customerName is required");
        }
        if (request.customerEmail() == null || request.customerEmail().isBlank() || !EMAIL_PATTERN.matcher(request.customerEmail().trim()).matches()) {
            throw new IllegalArgumentException("customerEmail must be a valid email format");
        }
        if (request.customerPhone() == null || request.customerPhone().isBlank()) {
            throw new IllegalArgumentException("customerPhone is required");
        }
        if (request.seats() == null || request.seats().isEmpty()) {
            throw new IllegalArgumentException("seats list must not be empty");
        }
        List<Long> seatIds = new ArrayList<>();
        for (BookingSeatRequest seat : request.seats()) {
            if (seat == null || seat.seatId() == null || seat.ticketType() == null) {
                throw new IllegalArgumentException("seatId and ticketType are required for all seats");
            }
            seatIds.add(seat.seatId());
        }
        if (new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new IllegalArgumentException("Duplicate seatIds are not allowed");
        }
    }

    private String generateUniqueBookingReference() {
        String ref;
        do {
            ref = "CB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (bookingRepository.existsByBookingReference(ref));
        return ref;
    }

    private record SeatPriceInfo(
            ShowtimeSeat showtimeSeat,
            com.cinebook.entity.enums.TicketType ticketType,
            BigDecimal price
    ) {}
}
