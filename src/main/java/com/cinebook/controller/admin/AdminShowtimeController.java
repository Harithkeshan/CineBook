package com.cinebook.controller.admin;

import com.cinebook.dto.ShowtimeCreateRequest;
import com.cinebook.dto.ShowtimeResponse;
import com.cinebook.dto.ShowtimeUpdateRequest;
import com.cinebook.service.ShowtimeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/showtimes")
public class AdminShowtimeController {

    private final ShowtimeService showtimeService;

    public AdminShowtimeController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @PostMapping
    public ResponseEntity<ShowtimeResponse> createShowtime(@RequestBody ShowtimeCreateRequest request) {
        ShowtimeResponse response = showtimeService.createShowtime(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> updateShowtime(@PathVariable Long id, @RequestBody ShowtimeUpdateRequest request) {
        ShowtimeResponse response = showtimeService.updateShowtime(id, request);
        return ResponseEntity.ok(response);
    }
}
