package com.cinebook.controller.admin;

import com.cinebook.dto.LocationCreateRequest;
import com.cinebook.dto.LocationResponse;
import com.cinebook.dto.LocationUpdateRequest;
import com.cinebook.entity.Location;
import com.cinebook.service.LocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/locations")
public class AdminLocationController {

    private final LocationService locationService;

    public AdminLocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    public ResponseEntity<LocationResponse> createLocation(@RequestBody LocationCreateRequest request) {
        Location location = locationService.createLocation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(location));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationResponse> updateLocation(@PathVariable Long id, @RequestBody LocationUpdateRequest request) {
        Location location = locationService.updateLocation(id, request);
        return ResponseEntity.ok(mapToResponse(location));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }

    private LocationResponse mapToResponse(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getAddress(),
                location.getCity()
        );
    }
}
