package com.cinebook.service;

import com.cinebook.entity.Ticket;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.TicketRepository;
import org.springframework.stereotype.Service;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
    }

    public Ticket getTicketByNumber(String ticketNumber) {
        return ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with number: " + ticketNumber));
    }

    public Ticket getTicketByQrToken(String qrToken) {
        return ticketRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with QR token: " + qrToken));
    }

    public Ticket getTicketByBookingSeatId(Long bookingSeatId) {
        return ticketRepository.findByBookingSeatId(bookingSeatId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found for booking seat id: " + bookingSeatId));
    }
}
