package com.cinebook.service;

import com.cinebook.dto.HoldSeatsRequest;
import com.cinebook.dto.SeatHoldResponse;
import com.cinebook.entity.Seat;
import com.cinebook.entity.Showtime;
import com.cinebook.entity.ShowtimeSeat;
import com.cinebook.entity.enums.ShowtimeSeatStatus;
import com.cinebook.entity.enums.ShowtimeStatus;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.exception.SeatUnavailableException;
import com.cinebook.repository.ShowtimeRepository;
import com.cinebook.repository.ShowtimeSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowtimeSeatServiceTest {

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private ShowtimeSeatRepository showtimeSeatRepository;

    private ShowtimeSeatService showtimeSeatService;

    @BeforeEach
    void setUp() {
        showtimeSeatService = new ShowtimeSeatService(showtimeRepository, showtimeSeatRepository, 5);
    }

    private Showtime createShowtime(Long id, ShowtimeStatus status) {
        Showtime showtime = new Showtime();
        showtime.setId(id);
        showtime.setStatus(status);
        return showtime;
    }

    private ShowtimeSeat createShowtimeSeat(Long id, Long seatId, ShowtimeSeatStatus status, OffsetDateTime holdExpiresAt) {
        Seat seat = new Seat();
        seat.setId(seatId);

        ShowtimeSeat ss = new ShowtimeSeat();
        ss.setId(id);
        ss.setSeat(seat);
        ss.setStatus(status);
        ss.setHoldExpiresAt(holdExpiresAt);
        return ss;
    }

    @Test
    void test1_SuccessfullyHoldSingleAvailableSeat() {
        Long showtimeId = 1L;
        Long seatId = 10L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);
        ShowtimeSeat ss = createShowtimeSeat(100L, seatId, ShowtimeSeatStatus.AVAILABLE, null);

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(seatId)))
                .thenReturn(List.of(ss));

        HoldSeatsRequest request = new HoldSeatsRequest(List.of(seatId));
        SeatHoldResponse response = showtimeSeatService.holdSeats(showtimeId, request);

        assertNotNull(response);
        assertEquals(showtimeId, response.showtimeId());
        assertEquals(List.of(seatId), response.seatIds());
        assertEquals(ShowtimeSeatStatus.HELD, response.status());
        assertNotNull(response.holdExpiresAt());
        assertEquals(ShowtimeSeatStatus.HELD, ss.getStatus());
    }

    @Test
    void test2_SuccessfullyHoldMultipleAvailableSeats() {
        Long showtimeId = 1L;
        List<Long> seatIds = List.of(10L, 11L);
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);
        ShowtimeSeat ss1 = createShowtimeSeat(100L, 10L, ShowtimeSeatStatus.AVAILABLE, null);
        ShowtimeSeat ss2 = createShowtimeSeat(101L, 11L, ShowtimeSeatStatus.AVAILABLE, null);

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, seatIds))
                .thenReturn(List.of(ss1, ss2));

        HoldSeatsRequest request = new HoldSeatsRequest(seatIds);
        SeatHoldResponse response = showtimeSeatService.holdSeats(showtimeId, request);

        assertNotNull(response);
        assertEquals(2, response.seatIds().size());
        assertEquals(ShowtimeSeatStatus.HELD, ss1.getStatus());
        assertEquals(ShowtimeSeatStatus.HELD, ss2.getStatus());
    }

    @Test
    void test3_RejectAlreadyHeldSeat() {
        Long showtimeId = 1L;
        Long seatId = 10L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);
        ShowtimeSeat ss = createShowtimeSeat(100L, seatId, ShowtimeSeatStatus.HELD, OffsetDateTime.now().plusMinutes(3));

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(seatId)))
                .thenReturn(List.of(ss));

        HoldSeatsRequest request = new HoldSeatsRequest(List.of(seatId));
        assertThrows(SeatUnavailableException.class, () -> showtimeSeatService.holdSeats(showtimeId, request));
    }

    @Test
    void test4_RejectAlreadyBookedSeat() {
        Long showtimeId = 1L;
        Long seatId = 10L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);
        ShowtimeSeat ss = createShowtimeSeat(100L, seatId, ShowtimeSeatStatus.BOOKED, null);

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(seatId)))
                .thenReturn(List.of(ss));

        HoldSeatsRequest request = new HoldSeatsRequest(List.of(seatId));
        assertThrows(SeatUnavailableException.class, () -> showtimeSeatService.holdSeats(showtimeId, request));
    }

    @Test
    void test5_ExpiredHeldSeatBecomesAvailableAndIsHeld() {
        Long showtimeId = 1L;
        Long seatId = 10L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);
        ShowtimeSeat ss = createShowtimeSeat(100L, seatId, ShowtimeSeatStatus.HELD, OffsetDateTime.now().minusMinutes(2));

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(seatId)))
                .thenReturn(List.of(ss));

        HoldSeatsRequest request = new HoldSeatsRequest(List.of(seatId));
        SeatHoldResponse response = showtimeSeatService.holdSeats(showtimeId, request);

        assertNotNull(response);
        assertEquals(ShowtimeSeatStatus.HELD, ss.getStatus());
        assertTrue(ss.getHoldExpiresAt().isAfter(OffsetDateTime.now()));
    }

    @Test
    void test6_RejectDuplicateSeatIds() {
        Long showtimeId = 1L;
        HoldSeatsRequest request = new HoldSeatsRequest(List.of(10L, 10L));
        assertThrows(IllegalArgumentException.class, () -> showtimeSeatService.holdSeats(showtimeId, request));
    }

    @Test
    void test7_RejectSeatBelongingToAnotherHall() {
        Long showtimeId = 1L;
        Long seatIdOtherHall = 50L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(seatIdOtherHall)))
                .thenReturn(List.of());

        HoldSeatsRequest request = new HoldSeatsRequest(List.of(seatIdOtherHall));
        assertThrows(ResourceNotFoundException.class, () -> showtimeSeatService.holdSeats(showtimeId, request));
    }

    @Test
    void test8_RejectNonexistentShowtime() {
        Long showtimeId = 999L;
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.empty());

        HoldSeatsRequest request = new HoldSeatsRequest(List.of(10L));
        assertThrows(ResourceNotFoundException.class, () -> showtimeSeatService.holdSeats(showtimeId, request));
    }

    @Test
    void test9_RejectNonexistentSeat() {
        Long showtimeId = 1L;
        Long nonexistentSeatId = 999L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, List.of(nonexistentSeatId)))
                .thenReturn(List.of());

        HoldSeatsRequest request = new HoldSeatsRequest(List.of(nonexistentSeatId));
        assertThrows(ResourceNotFoundException.class, () -> showtimeSeatService.holdSeats(showtimeId, request));
    }

    @Test
    void test10_RejectCancelledShowtime() {
        Long showtimeId = 1L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.CANCELLED);
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));

        HoldSeatsRequest request = new HoldSeatsRequest(List.of(10L));
        assertThrows(IllegalStateException.class, () -> showtimeSeatService.holdSeats(showtimeId, request));
    }

    @Test
    void test11_RejectCompletedShowtime() {
        Long showtimeId = 1L;
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.COMPLETED);
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));

        HoldSeatsRequest request = new HoldSeatsRequest(List.of(10L));
        assertThrows(IllegalStateException.class, () -> showtimeSeatService.holdSeats(showtimeId, request));
    }

    @Test
    void test12_AtomicityPartialFailureRejectsAllSeats() {
        Long showtimeId = 1L;
        List<Long> seatIds = List.of(10L, 11L);
        Showtime showtime = createShowtime(showtimeId, ShowtimeStatus.SCHEDULED);
        ShowtimeSeat ss1 = createShowtimeSeat(100L, 10L, ShowtimeSeatStatus.AVAILABLE, null);
        ShowtimeSeat ss2 = createShowtimeSeat(101L, 11L, ShowtimeSeatStatus.BOOKED, null);

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(showtimeId, seatIds))
                .thenReturn(List.of(ss1, ss2));

        HoldSeatsRequest request = new HoldSeatsRequest(seatIds);
        assertThrows(SeatUnavailableException.class, () -> showtimeSeatService.holdSeats(showtimeId, request));

        assertEquals(ShowtimeSeatStatus.AVAILABLE, ss1.getStatus());
        verify(showtimeSeatRepository, never()).save(any());
    }
}
