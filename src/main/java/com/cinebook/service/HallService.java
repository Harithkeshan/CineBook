package com.cinebook.service;

import com.cinebook.dto.HallCreateRequest;
import com.cinebook.dto.HallUpdateRequest;
import com.cinebook.entity.Hall;
import com.cinebook.entity.Location;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.HallRepository;
import com.cinebook.repository.LocationRepository;
import com.cinebook.repository.SectionRepository;
import com.cinebook.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HallService {

    private final HallRepository hallRepository;
    private final LocationRepository locationRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SectionRepository sectionRepository;

    public HallService(
            HallRepository hallRepository,
            LocationRepository locationRepository,
            ShowtimeRepository showtimeRepository,
            SectionRepository sectionRepository
    ) {
        this.hallRepository = hallRepository;
        this.locationRepository = locationRepository;
        this.showtimeRepository = showtimeRepository;
        this.sectionRepository = sectionRepository;
    }

    public List<Hall> getHallsByLocationId(Long locationId) {
        return hallRepository.findByLocationId(locationId);
    }

    public Hall getHallById(Long id) {
        return hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + id));
    }

    @Transactional
    public Hall createHall(Long locationId, HallCreateRequest request) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + locationId));

        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Hall name is required");
        }

        Hall hall = new Hall();
        hall.setLocation(location);
        hall.setName(request.name().trim());

        return hallRepository.save(hall);
    }

    @Transactional
    public Hall updateHall(Long id, HallUpdateRequest request) {
        Hall hall = getHallById(id);

        if (request != null && request.name() != null && !request.name().isBlank()) {
            hall.setName(request.name().trim());
        }

        return hallRepository.save(hall);
    }

    @Transactional
    public void deleteHall(Long id) {
        Hall hall = getHallById(id);

        if (showtimeRepository.existsByHallId(id) || sectionRepository.existsByHallId(id)) {
            throw new IllegalStateException("Cannot delete hall because it is referenced by showtimes or sections");
        }

        hallRepository.delete(hall);
    }
}
