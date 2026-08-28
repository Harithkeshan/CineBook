package com.cinebook.service;

import com.cinebook.entity.Seat;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.SeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatService {

    private final SeatRepository seatRepository;

    public SeatService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    public List<Seat> getSeatsBySectionId(Long sectionId) {
        return seatRepository.findBySectionId(sectionId);
    }

    public Seat getSeatById(Long id) {
        return seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));
    }
}
