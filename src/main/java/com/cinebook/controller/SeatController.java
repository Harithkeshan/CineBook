package com.cinebook.controller;

import com.cinebook.dto.SeatResponse;
import com.cinebook.entity.Seat;
import com.cinebook.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping("/sections/{sectionId}/seats")
    public ResponseEntity<List<SeatResponse>> getSeatsBySectionId(@PathVariable Long sectionId) {
        List<SeatResponse> seats = seatService.getSeatsBySectionId(sectionId).stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/seats/{id}")
    public ResponseEntity<SeatResponse> getSeatById(@PathVariable Long id) {
        Seat seat = seatService.getSeatById(id);
        return ResponseEntity.ok(mapToResponse(seat));
    }

    private SeatResponse mapToResponse(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getSection() != null ? seat.getSection().getId() : null,
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getSeatType(),
                seat.getPositionX(),
                seat.getPositionY(),
                seat.getIsActive()
        );
    }
}
