package com.cinebook.controller;

import com.cinebook.dto.ShowtimeResponse;
import com.cinebook.entity.Showtime;
import com.cinebook.service.ShowtimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    public ShowtimeController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
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
