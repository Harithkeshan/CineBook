package com.cinebook.dto;

public record ErrorResponse(
    int status,
    String message
) {}
