package com.cinebook.controller;

import com.cinebook.dto.CancelShowtimeResponse;
import com.cinebook.dto.HoldSeatsRequest;
import com.cinebook.dto.SeatHoldResponse;
import com.cinebook.dto.ShowtimeResponse;
import com.cinebook.dto.ShowtimeSeatResponse;
import com.cinebook.entity.Showtime;
import com.cinebook.service.ShowtimeSeatService;
import com.cinebook.service.ShowtimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ShowtimeController {

    private final ShowtimeService showtimeService;
    private final ShowtimeSeatService showtimeSeatService;

    public ShowtimeController(ShowtimeService showtimeService, ShowtimeSeatService showtimeSeatService) {
        this.showtimeService = showtimeService;
        this.showtimeSeatService = showtimeSeatService;
    }

    @GetMapping("/movies/{movieId}/showtimes")
    public ResponseEntity<List<ShowtimeResponse>> getShowtimesByMovieId(@PathVariable Long movieId) {
        List<ShowtimeResponse> showtimes = showtimeService.getShowtimesByMovieId(movieId).stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(showtimes);
    }

    @GetMapping("/showtimes/{id}")
    public ResponseEntity<ShowtimeResponse> getShowtimeById(@PathVariable Long id) {
        Showtime showtime = showtimeService.getShowtimeById(id);
        return ResponseEntity.ok(mapToResponse(showtime));
    }

    @GetMapping("/halls/{hallId}/showtimes")
    public ResponseEntity<List<ShowtimeResponse>> getShowtimesByHallId(@PathVariable Long hallId) {
        List<ShowtimeResponse> showtimes = showtimeService.getShowtimesByHallId(hallId).stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(showtimes);
    }

    @GetMapping("/showtimes/{showtimeId}/seats")
    public ResponseEntity<List<ShowtimeSeatResponse>> getSeatsByShowtimeId(@PathVariable Long showtimeId) {
        List<ShowtimeSeatResponse> seats = showtimeSeatService.getSeatsByShowtimeId(showtimeId);
        return ResponseEntity.ok(seats);
    }

    @PostMapping("/showtimes/{showtimeId}/seats/hold")
    public ResponseEntity<SeatHoldResponse> holdSeats(
            @PathVariable Long showtimeId,
            @RequestBody HoldSeatsRequest request
    ) {
        SeatHoldResponse response = showtimeSeatService.holdSeats(showtimeId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/showtimes/{showtimeId}/cancel")
    public ResponseEntity<CancelShowtimeResponse> cancelShowtimeByCinema(@PathVariable Long showtimeId) {
        CancelShowtimeResponse response = showtimeService.cancelShowtimeByCinema(showtimeId);
        return ResponseEntity.ok(response);
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
