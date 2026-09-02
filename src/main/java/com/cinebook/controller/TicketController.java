package com.cinebook.controller;

import com.cinebook.dto.TicketCheckInResponse;
import com.cinebook.dto.TicketResponse;
import com.cinebook.dto.TicketVerificationResponse;
import com.cinebook.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/bookings/{bookingReference}/tickets")
    public ResponseEntity<List<TicketResponse>> getTicketsByBookingReference(@PathVariable String bookingReference) {
        List<TicketResponse> tickets = ticketService.getTicketsByBookingReference(bookingReference);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/tickets/{ticketNumber}")
    public ResponseEntity<TicketResponse> getTicketByNumber(@PathVariable String ticketNumber) {
        TicketResponse ticket = ticketService.getTicketByNumberDto(ticketNumber);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/tickets/verify/{qrToken}")
    public ResponseEntity<TicketVerificationResponse> verifyTicketByQrToken(@PathVariable String qrToken) {
        TicketVerificationResponse verification = ticketService.verifyTicketByQrToken(qrToken);
        return ResponseEntity.ok(verification);
    }

    @PostMapping("/tickets/check-in/{qrToken}")
    public ResponseEntity<TicketCheckInResponse> checkInTicket(@PathVariable String qrToken) {
        TicketCheckInResponse checkInResponse = ticketService.checkInTicket(qrToken);
        return ResponseEntity.ok(checkInResponse);
    }
}
