package com.cinebook.service;

import com.cinebook.dto.SeatCreateRequest;
import com.cinebook.entity.Seat;
import com.cinebook.entity.Section;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.SeatRepository;
import com.cinebook.repository.SectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final SectionRepository sectionRepository;

    public SeatService(SeatRepository seatRepository, SectionRepository sectionRepository) {
        this.seatRepository = seatRepository;
        this.sectionRepository = sectionRepository;
    }

    public List<Seat> getSeatsBySectionId(Long sectionId) {
        return seatRepository.findBySectionId(sectionId);
    }

    public Seat getSeatById(Long id) {
        return seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + id));
    }

    @Transactional
    public Seat createSeat(Long sectionId, SeatCreateRequest request) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + sectionId));

        if (request == null || request.rowLabel() == null || request.rowLabel().isBlank()) {
            throw new IllegalArgumentException("rowLabel is required");
        }
        if (request.seatNumber() == null || request.seatNumber().isBlank()) {
            throw new IllegalArgumentException("seatNumber is required");
        }
        if (request.seatType() == null) {
            throw new IllegalArgumentException("seatType is required");
        }

        String rowLabel = request.rowLabel().trim();
        String seatNumber = request.seatNumber().trim();

        if (seatRepository.existsBySectionIdAndRowLabelAndSeatNumber(sectionId, rowLabel, seatNumber)) {
            throw new IllegalStateException("Seat already exists in section with row label " + rowLabel + " and seat number " + seatNumber);
        }

        Seat seat = new Seat();
        seat.setSection(section);
        seat.setRowLabel(rowLabel);
        seat.setSeatNumber(seatNumber);
        seat.setSeatType(request.seatType());
        seat.setPositionX(request.positionX() != null ? request.positionX() : 0);
        seat.setPositionY(request.positionY() != null ? request.positionY() : 0);
        seat.setIsActive(request.isActive() != null ? request.isActive() : true);

        return seatRepository.save(seat);
    }
}
