package com.cinebook.service;

import com.cinebook.entity.Section;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.SectionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;

    public SectionService(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    public List<Section> getSectionsByHallId(Long hallId) {
        return sectionRepository.findByHallId(hallId);
    }

    public Section getSectionById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + id));
    }
}
