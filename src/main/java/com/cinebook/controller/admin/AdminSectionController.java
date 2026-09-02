package com.cinebook.controller.admin;

import com.cinebook.dto.SectionCreateRequest;
import com.cinebook.dto.SectionResponse;
import com.cinebook.entity.Section;
import com.cinebook.service.SectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminSectionController {

    private final SectionService sectionService;

    public AdminSectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @PostMapping("/halls/{hallId}/sections")
    public ResponseEntity<SectionResponse> createSection(@PathVariable Long hallId, @RequestBody SectionCreateRequest request) {
        Section section = sectionService.createSection(hallId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(section));
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
