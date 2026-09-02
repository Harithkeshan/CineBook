package com.cinebook.dto;

public record LocationUpdateRequest(
    String name,
    String address,
    String city
) {}
