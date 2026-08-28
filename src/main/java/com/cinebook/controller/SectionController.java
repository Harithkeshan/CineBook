package com.cinebook.controller;

import com.cinebook.dto.SectionResponse;
import com.cinebook.entity.Section;
import com.cinebook.service.SectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping("/halls/{hallId}/sections")
    public ResponseEntity<List<SectionResponse>> getSectionsByHallId(@PathVariable Long hallId) {
        List<SectionResponse> sections = sectionService.getSectionsByHallId(hallId).stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(sections);
    }

    @GetMapping("/sections/{id}")
    public ResponseEntity<SectionResponse> getSectionById(@PathVariable Long id) {
        Section section = sectionService.getSectionById(id);
        return ResponseEntity.ok(mapToResponse(section));
    }

    private SectionResponse mapToResponse(Section section) {
        return new SectionResponse(
                section.getId(),
                section.getHall() != null ? section.getHall().getId() : null,
                section.getName(),
                section.getType()
        );
    }
}
