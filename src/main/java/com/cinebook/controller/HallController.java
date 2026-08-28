package com.cinebook.controller;

import com.cinebook.dto.HallResponse;
import com.cinebook.entity.Hall;
import com.cinebook.service.HallService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class HallController {

    private final HallService hallService;

    public HallController(HallService hallService) {
        this.hallService = hallService;
    }

    @GetMapping("/locations/{locationId}/halls")
    public ResponseEntity<List<HallResponse>> getHallsByLocationId(@PathVariable Long locationId) {
        List<HallResponse> halls = hallService.getHallsByLocationId(locationId).stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(halls);
    }

    @GetMapping("/halls/{id}")
    public ResponseEntity<HallResponse> getHallById(@PathVariable Long id) {
        Hall hall = hallService.getHallById(id);
        return ResponseEntity.ok(mapToResponse(hall));
    }

    private HallResponse mapToResponse(Hall hall) {
        return new HallResponse(
                hall.getId(),
                hall.getLocation() != null ? hall.getLocation().getId() : null,
                hall.getName()
        );
    }
}
