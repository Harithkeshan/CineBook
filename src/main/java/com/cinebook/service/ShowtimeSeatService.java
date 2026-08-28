package com.cinebook.service;

import com.cinebook.dto.HoldSeatsRequest;
import com.cinebook.dto.SeatHoldResponse;
import com.cinebook.dto.ShowtimeSeatResponse;
import com.cinebook.entity.Seat;
import com.cinebook.entity.Section;
import com.cinebook.entity.Showtime;
import com.cinebook.entity.ShowtimeSeat;
import com.cinebook.entity.enums.ShowtimeSeatStatus;
import com.cinebook.entity.enums.ShowtimeStatus;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.exception.SeatUnavailableException;
import com.cinebook.repository.ShowtimeRepository;
import com.cinebook.repository.ShowtimeSeatRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;

@Service
public class ShowtimeSeatService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final int seatHoldMinutes;

    public ShowtimeSeatService(
            ShowtimeRepository showtimeRepository,
            ShowtimeSeatRepository showtimeSeatRepository,
            @Value("${cinebook.booking.seat-hold-minutes:5}") int seatHoldMinutes
    ) {
        this.showtimeRepository = showtimeRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.seatHoldMinutes = seatHoldMinutes;
    }

    public List<ShowtimeSeatResponse> getSeatsByShowtimeId(Long showtimeId) {
        if (!showtimeRepository.existsById(showtimeId)) {
            throw new ResourceNotFoundException("Showtime not found with id: " + showtimeId);
        }

        List<ShowtimeSeat> showtimeSeats = showtimeSeatRepository.findByShowtimeIdWithSeatAndSectionOrdered(showtimeId);
        return showtimeSeats.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public SeatHoldResponse holdSeats(Long showtimeId, HoldSeatsRequest request) {
        if (request == null || request.seatIds() == null || request.seatIds().isEmpty()) {
            throw new IllegalArgumentException("seatIds must not be empty");
        }

        List<Long> seatIds = request.seatIds();
        if (new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new IllegalArgumentException("Duplicate seatIds are not allowed");
        }

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + showtimeId));

        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot hold seats for a showtime with status: " + showtime.getStatus());
        }

        List<ShowtimeSeat> lockedShowtimeSeats = showtimeSeatRepository
                .findByShowtimeIdAndSeatIdInWithLock(showtimeId, seatIds);

        if (lockedShowtimeSeats.size() != seatIds.size()) {
            throw new ResourceNotFoundException("One or more requested seats do not exist or do not belong to the showtime's hall");
        }

        OffsetDateTime now = OffsetDateTime.now();

        for (ShowtimeSeat ss : lockedShowtimeSeats) {
            if (ss.getStatus() == ShowtimeSeatStatus.BOOKED) {
                throw new SeatUnavailableException("Seat is already booked: " + ss.getSeat().getId());
            }
            if (ss.getStatus() == ShowtimeSeatStatus.HELD && ss.getHoldExpiresAt() != null && ss.getHoldExpiresAt().isAfter(now)) {
                throw new SeatUnavailableException("Seat is currently held by another session: " + ss.getSeat().getId());
            }
        }

        OffsetDateTime expiresAt = now.plusMinutes(seatHoldMinutes);

        for (ShowtimeSeat ss : lockedShowtimeSeats) {
            ss.setStatus(ShowtimeSeatStatus.HELD);
            ss.setHoldExpiresAt(expiresAt);
            showtimeSeatRepository.save(ss);
        }

        return new SeatHoldResponse(showtimeId, seatIds, ShowtimeSeatStatus.HELD, expiresAt);
    }

    private ShowtimeSeatResponse mapToResponse(ShowtimeSeat showtimeSeat) {
        Seat seat = showtimeSeat.getSeat();
        Section section = seat != null ? seat.getSection() : null;

        // Lazy expiration check for read-only endpoint
        ShowtimeSeatStatus currentStatus = showtimeSeat.getStatus();
        if (currentStatus == ShowtimeSeatStatus.HELD
                && showtimeSeat.getHoldExpiresAt() != null
                && showtimeSeat.getHoldExpiresAt().isBefore(OffsetDateTime.now())) {
            currentStatus = ShowtimeSeatStatus.AVAILABLE;
        }

        return new ShowtimeSeatResponse(
                showtimeSeat.getId(),
                seat != null ? seat.getId() : null,
                section != null ? section.getId() : null,
                section != null ? section.getName() : null,
                section != null ? section.getType() : null,
                seat != null ? seat.getRowLabel() : null,
                seat != null ? seat.getSeatNumber() : null,
                seat != null ? seat.getSeatType() : null,
                seat != null ? seat.getPositionX() : null,
                seat != null ? seat.getPositionY() : null,
                currentStatus
        );
    }
}
