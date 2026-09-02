package com.cinebook.dto;

import com.cinebook.entity.enums.SectionType;

public record SectionCreateRequest(
    String name,
    SectionType type
) {}
