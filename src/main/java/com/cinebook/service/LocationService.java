package com.cinebook.service;

import com.cinebook.dto.LocationCreateRequest;
import com.cinebook.dto.LocationUpdateRequest;
import com.cinebook.entity.Location;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.HallRepository;
import com.cinebook.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final HallRepository hallRepository;

    public LocationService(LocationRepository locationRepository, HallRepository hallRepository) {
        this.locationRepository = locationRepository;
        this.hallRepository = hallRepository;
    }

    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }

    public Location getLocationById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + id));
    }

    @Transactional
    public Location createLocation(LocationCreateRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Location name is required");
        }

        Location location = new Location();
        location.setName(request.name().trim());
        location.setAddress(request.address());
        location.setCity(request.city());

        return locationRepository.save(location);
    }

    @Transactional
    public Location updateLocation(Long id, LocationUpdateRequest request) {
        Location location = getLocationById(id);

        if (request != null) {
            if (request.name() != null && !request.name().isBlank()) {
                location.setName(request.name().trim());
            }
            location.setAddress(request.address());
            location.setCity(request.city());
        }

        return locationRepository.save(location);
    }

    @Transactional
    public void deleteLocation(Long id) {
        Location location = getLocationById(id);
        if (hallRepository.existsByLocationId(id)) {
            throw new IllegalStateException("Cannot delete location because it has associated halls");
        }
        locationRepository.delete(location);
    }
}
