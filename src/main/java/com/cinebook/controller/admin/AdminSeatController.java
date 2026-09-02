package com.cinebook.controller.admin;

import com.cinebook.dto.SeatCreateRequest;
import com.cinebook.dto.SeatResponse;
import com.cinebook.entity.Seat;
import com.cinebook.service.SeatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminSeatController {

    private final SeatService seatService;

    public AdminSeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping("/sections/{sectionId}/seats")
    public ResponseEntity<SeatResponse> createSeat(@PathVariable Long sectionId, @RequestBody SeatCreateRequest request) {
        Seat seat = seatService.createSeat(sectionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(seat));
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
