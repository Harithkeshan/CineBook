package com.cinebook.service;

import com.cinebook.dto.TicketCheckInResponse;
import com.cinebook.entity.Booking;
import com.cinebook.entity.BookingSeat;
import com.cinebook.entity.Seat;
import com.cinebook.entity.Ticket;
import com.cinebook.entity.enums.BookingStatus;
import com.cinebook.entity.enums.TicketStatus;
import com.cinebook.entity.enums.TicketType;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketCheckInConcurrencyTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private BookingRepository bookingRepository;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, bookingRepository);
    }

    @Test
    void testConcurrentCheckIn_OnlyOneSucceeds() throws Exception {
        String qrToken = "CONCURRENT-QR-TOKEN";

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference("CB-CONCURRENT");
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingSeats(new ArrayList<>());

        Seat seat = new Seat();
        seat.setId(10L);
        seat.setRowLabel("A");
        seat.setSeatNumber("1");

        BookingSeat bs = new BookingSeat();
        bs.setId(100L);
        bs.setBooking(booking);
        bs.setTicketType(TicketType.ADULT);

        Ticket sharedTicket = new Ticket(bs, "TKT-CONCURRENT-01", qrToken, TicketStatus.ACTIVE);

        when(ticketRepository.findByQrTokenWithLock(qrToken)).thenReturn(Optional.of(sharedTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setUsedAt(OffsetDateTime.now());
            return t;
        });

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CyclicBarrier barrier = new CyclicBarrier(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    synchronized (sharedTicket) {
                        ticketService.checkInTicket(qrToken);
                    }
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    if ("Ticket has already been used.".equals(e.getMessage())) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // unexpected exception
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }

        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one check-in must succeed");
        assertEquals(numThreads - 1, conflictCount.get(), "All other concurrent attempts must be rejected with conflict");
        assertEquals(TicketStatus.USED, sharedTicket.getStatus());
        assertNotNull(sharedTicket.getUsedAt());
    }
}
