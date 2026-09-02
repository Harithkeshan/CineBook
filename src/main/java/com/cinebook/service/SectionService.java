package com.cinebook.service;

import com.cinebook.dto.SectionCreateRequest;
import com.cinebook.entity.Hall;
import com.cinebook.entity.Section;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.HallRepository;
import com.cinebook.repository.SectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final HallRepository hallRepository;

    public SectionService(SectionRepository sectionRepository, HallRepository hallRepository) {
        this.sectionRepository = sectionRepository;
        this.hallRepository = hallRepository;
    }

    public List<Section> getSectionsByHallId(Long hallId) {
        return sectionRepository.findByHallId(hallId);
    }

    public Section getSectionById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + id));
    }

    @Transactional
    public Section createSection(Long hallId, SectionCreateRequest request) {
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id: " + hallId));

        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Section name is required");
        }
        if (request.type() == null) {
            throw new IllegalArgumentException("Section type is required");
        }

        Section section = new Section();
        section.setHall(hall);
        section.setName(request.name().trim());
        section.setType(request.type());

        return sectionRepository.save(section);
    }
}
