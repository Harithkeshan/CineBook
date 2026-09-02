package com.cinebook.service;

import com.cinebook.dto.CancelShowtimeResponse;
import com.cinebook.dto.ShowtimeCreateRequest;
import com.cinebook.dto.ShowtimeResponse;
import com.cinebook.dto.ShowtimeUpdateRequest;
import com.cinebook.entity.Booking;
import com.cinebook.entity.BookingSeat;
import com.cinebook.entity.Hall;
import com.cinebook.entity.Movie;
import com.cinebook.entity.Payment;
import com.cinebook.entity.Refund;
import com.cinebook.entity.Seat;
import com.cinebook.entity.Showtime;
import com.cinebook.entity.ShowtimeSeat;
import com.cinebook.entity.Ticket;
import com.cinebook.entity.enums.BookingStatus;
import com.cinebook.entity.enums.PaymentStatus;
import com.cinebook.entity.enums.ShowtimeSeatStatus;
import com.cinebook.entity.enums.ShowtimeStatus;
import com.cinebook.entity.enums.TicketStatus;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.HallRepository;
import com.cinebook.repository.MovieRepository;
import com.cinebook.repository.PaymentRepository;
import com.cinebook.repository.SeatRepository;
import com.cinebook.repository.ShowtimeRepository;
import com.cinebook.repository.ShowtimeSeatRepository;
import com.cinebook.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundService refundService;
    private final TicketRepository ticketRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final MovieRepository movieRepository;
    private final HallRepository hallRepository;
    private final SeatRepository seatRepository;

    public ShowtimeService(
            ShowtimeRepository showtimeRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            RefundService refundService,
            TicketRepository ticketRepository,
            ShowtimeSeatRepository showtimeSeatRepository,
            MovieRepository movieRepository,
            HallRepository hallRepository,
            SeatRepository seatRepository
    ) {
        this.showtimeRepository = showtimeRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.refundService = refundService;
        this.ticketRepository = ticketRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.movieRepository = movieRepository;
        this.hallRepository = hallRepository;
        this.seatRepository = seatRepository;
    }

    public List<Showtime> getAllShowtimes() {
        return showtimeRepository.findAll();
    }

    public List<Showtime> getShowtimesByMovieId(Long movieId) {
        return showtimeRepository.findByMovieId(movieId);
    }

    public List<Showtime> getShowtimesByHallId(Long hallId) {
        return showtimeRepository.findByHallId(hallId);
    }

    public List<Showtime> getShowtimesByMovieIdAndHallId(Long movieId, Long hallId) {
        return showtimeRepository.findByMovieIdAndHallId(movieId, hallId);
    }

    public Showtime getShowtimeById(Long id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + id));
    }

    @Transactional
    public ShowtimeResponse createShowtime(ShowtimeCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Showtime request must not be null");
        }
        if (request.movieId() == null) {
            throw new IllegalArgumentException("movieId is required");
        }
        if (request.hallId() == null) {
            throw new IllegalArgumentException("hallId is required");
        }
        if (request.startTime() == null || request.endTime() == null) {
            throw new IllegalArgumentException("startTime and endTime are required");
        }

        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + request.movieId()));

        Hall hall = hallRepository.findById(request.hallId())
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + request.hallId()));

        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("Showtime endTime must be after startTime");
        }

        List<Showtime> overlaps = showtimeRepository.findOverlappingShowtimes(request.hallId(), request.startTime(), request.endTime());
        if (!overlaps.isEmpty()) {
            throw new IllegalStateException("Showtime overlaps with an existing showtime in the same hall.");
        }

        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setHall(hall);
        showtime.setStartTime(request.startTime());
        showtime.setEndTime(request.endTime());
        showtime.setStatus(ShowtimeStatus.SCHEDULED);

        showtime = showtimeRepository.save(showtime);

        List<Seat> activeSeats = seatRepository.findBySectionHallIdAndIsActiveTrue(request.hallId());
        for (Seat seat : activeSeats) {
            ShowtimeSeat ss = new ShowtimeSeat();
            ss.setShowtime(showtime);
            ss.setSeat(seat);
            ss.setStatus(ShowtimeSeatStatus.AVAILABLE);
            ss.setHoldExpiresAt(null);
            showtimeSeatRepository.save(ss);
        }

        return mapToResponse(showtime);
    }

    @Transactional
    public ShowtimeResponse updateShowtime(Long id, ShowtimeUpdateRequest request) {
        Showtime showtime = getShowtimeById(id);

        if (showtime.getStatus() == ShowtimeStatus.COMPLETED) {
            throw new IllegalStateException("Cannot update a completed showtime.");
        }

        if (request != null) {
            if (request.movieId() != null) {
                Movie movie = movieRepository.findById(request.movieId())
                        .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + request.movieId()));
                showtime.setMovie(movie);
            }
            if (request.hallId() != null) {
                Hall hall = hallRepository.findById(request.hallId())
                        .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + request.hallId()));
                showtime.setHall(hall);
            }
            if (request.startTime() != null) {
                showtime.setStartTime(request.startTime());
            }
            if (request.endTime() != null) {
                showtime.setEndTime(request.endTime());
            }

            if (!showtime.getEndTime().isAfter(showtime.getStartTime())) {
                throw new IllegalArgumentException("Showtime endTime must be after startTime");
            }

            List<Showtime> overlaps = showtimeRepository.findOverlappingShowtimesExcludingId(
                    showtime.getHall().getId(),
                    showtime.getStartTime(),
                    showtime.getEndTime(),
                    id
            );
            if (!overlaps.isEmpty()) {
                throw new IllegalStateException("Showtime overlaps with an existing showtime in the same hall.");
            }
        }

        showtime = showtimeRepository.save(showtime);
        return mapToResponse(showtime);
    }

    @Transactional
    public CancelShowtimeResponse cancelShowtimeByCinema(Long showtimeId) {
        Showtime showtime = getShowtimeById(showtimeId);

        if (showtime.getStatus() == ShowtimeStatus.CANCELLED) {
            throw new IllegalStateException("Showtime is already cancelled");
        }
        if (showtime.getStatus() == ShowtimeStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed showtime");
        }
        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot cancel showtime with status: " + showtime.getStatus());
        }

        showtime.setStatus(ShowtimeStatus.CANCELLED);
        showtimeRepository.save(showtime);

        List<Booking> bookings = bookingRepository.findByShowtimeId(showtimeId);
        int totalBookingsCancelled = 0;
        int totalRefundsProcessed = 0;
        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.CANCELLED) {
                continue;
            }

            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            totalBookingsCancelled++;

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

            List<Payment> paidPayments = paymentRepository.findByBookingIdAndStatus(booking.getId(), PaymentStatus.PAID);
            for (Payment payment : paidPayments) {
                Refund refund = refundService.processShowCancellationRefund(payment);
                if (refund != null) {
                    totalRefundsProcessed++;
                    totalRefundAmount = totalRefundAmount.add(refund.getAmount());
                }
            }
        }

        return new CancelShowtimeResponse(
                showtimeId,
                ShowtimeStatus.CANCELLED,
                totalBookingsCancelled,
                totalRefundsProcessed,
                totalRefundAmount,
                "Showtime cancelled successfully. All affected bookings have been cancelled and eligible payments refunded."
        );
    }

    private ShowtimeResponse mapToResponse(Showtime showtime) {
        return new ShowtimeResponse(
                showtime.getId(),
                showtime.getMovie() != null ? showtime.getMovie().getId() : null,
                showtime.getHall() != null ? showtime.getHall().getId() : null,
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getStatus()
        );
    }
}
