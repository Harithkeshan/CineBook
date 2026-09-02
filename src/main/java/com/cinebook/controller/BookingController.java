package com.cinebook.controller;

import com.cinebook.dto.BookingDetailsResponse;
import com.cinebook.dto.BookingRequest;
import com.cinebook.dto.BookingResponse;
import com.cinebook.dto.BookingSummaryResponse;
import com.cinebook.dto.CancelBookingResponse;
import com.cinebook.dto.RefundResponse;
import com.cinebook.service.BookingService;
import com.cinebook.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;
    private final RefundService refundService;

    public BookingController(BookingService bookingService, RefundService refundService) {
        this.bookingService = bookingService;
        this.refundService = refundService;
    }

    @PostMapping("/showtimes/{showtimeId}/bookings")
    public ResponseEntity<BookingResponse> createGuestBooking(
            @PathVariable Long showtimeId,
            @Valid @RequestBody BookingRequest request
    ) {
        BookingResponse response = bookingService.createGuestBooking(showtimeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/bookings/{bookingReference}")
    public ResponseEntity<BookingDetailsResponse> getBookingDetailsByReference(@PathVariable String bookingReference) {
        BookingDetailsResponse response = bookingService.getBookingDetailsByReference(bookingReference);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}/bookings")
    public ResponseEntity<Page<BookingSummaryResponse>> getUserBookingHistory(
            @PathVariable Long userId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<BookingSummaryResponse> response = bookingService.getUserBookingHistory(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bookings/{bookingReference}/cancel")
    public ResponseEntity<CancelBookingResponse> cancelBookingByCustomer(@PathVariable String bookingReference) {
        CancelBookingResponse response = bookingService.cancelBookingByCustomer(bookingReference);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings/{bookingReference}/refunds")
    public ResponseEntity<List<RefundResponse>> getRefundsByBookingReference(@PathVariable String bookingReference) {
        List<RefundResponse> refunds = refundService.getRefundsByBookingReference(bookingReference);
        return ResponseEntity.ok(refunds);
    }
}
