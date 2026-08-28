package com.cinebook.service;

import com.cinebook.entity.Hall;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.HallRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HallService {

    private final HallRepository hallRepository;

    public HallService(HallRepository hallRepository) {
        this.hallRepository = hallRepository;
    }

    public List<Hall> getHallsByLocationId(Long locationId) {
        return hallRepository.findByLocationId(locationId);
    }

    public Hall getHallById(Long id) {
        return hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + id));
    }
}
