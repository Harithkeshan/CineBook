package com.cinebook.dto;

public record LocationCreateRequest(
    String name,
    String address,
    String city
) {}
