package com.cinebook.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BookingRequest(
    @NotBlank(message = "customerName is required")
    String customerName,

    @NotBlank(message = "customerEmail is required")
    @Email(message = "customerEmail must be a valid email format")
    String customerEmail,

    @NotBlank(message = "customerPhone is required")
    String customerPhone,

    @NotEmpty(message = "seats list must not be empty")
    @Valid
    List<BookingSeatRequest> seats
) {}
