package com.cinebook.controller;

import com.cinebook.dto.BookingRequest;
import com.cinebook.dto.BookingResponse;
import com.cinebook.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/showtimes/{showtimeId}/bookings")
    public ResponseEntity<BookingResponse> createGuestBooking(
            @PathVariable Long showtimeId,
            @Valid @RequestBody BookingRequest request
    ) {
        BookingResponse response = bookingService.createGuestBooking(showtimeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
