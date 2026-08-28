package com.cinebook.dto;

public record LocationResponse(
    Long id,
    String name,
    String address,
    String city
) {}
