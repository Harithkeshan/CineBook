package com.cinebook.dto;

import java.time.LocalDate;

public record MovieUpdateRequest(
    String title,
    String description,
    Integer durationMinutes,
    String language,
    String genre,
    String posterUrl,
    String trailerUrl,
    LocalDate releaseDate,
    String ageRating
) {}
