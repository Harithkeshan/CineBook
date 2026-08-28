package com.cinebook.dto;

import com.cinebook.entity.enums.SectionType;

public record SectionResponse(
    Long id,
    Long hallId,
    String name,
    SectionType type
) {}
