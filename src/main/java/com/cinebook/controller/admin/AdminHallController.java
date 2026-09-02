package com.cinebook.controller.admin;

import com.cinebook.dto.HallCreateRequest;
import com.cinebook.dto.HallResponse;
import com.cinebook.dto.HallUpdateRequest;
import com.cinebook.entity.Hall;
import com.cinebook.service.HallService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminHallController {

    private final HallService hallService;

    public AdminHallController(HallService hallService) {
        this.hallService = hallService;
    }

    @PostMapping("/locations/{locationId}/halls")
    public ResponseEntity<HallResponse> createHall(@PathVariable Long locationId, @RequestBody HallCreateRequest request) {
        Hall hall = hallService.createHall(locationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(hall));
    }

    @PutMapping("/halls/{id}")
    public ResponseEntity<HallResponse> updateHall(@PathVariable Long id, @RequestBody HallUpdateRequest request) {
        Hall hall = hallService.updateHall(id, request);
        return ResponseEntity.ok(mapToResponse(hall));
    }

    @DeleteMapping("/halls/{id}")
    public ResponseEntity<Void> deleteHall(@PathVariable Long id) {
        hallService.deleteHall(id);
        return ResponseEntity.noContent().build();
    }

    private HallResponse mapToResponse(Hall hall) {
        return new HallResponse(
                hall.getId(),
                hall.getLocation() != null ? hall.getLocation().getId() : null,
                hall.getName()
        );
    }
}
