package com.cinebook.service;

import com.cinebook.dto.HoldSeatsRequest;
import com.cinebook.entity.Seat;
import com.cinebook.entity.Showtime;
import com.cinebook.entity.ShowtimeSeat;
import com.cinebook.entity.enums.ShowtimeSeatStatus;
import com.cinebook.entity.enums.ShowtimeStatus;
import com.cinebook.exception.SeatUnavailableException;
import com.cinebook.repository.ShowtimeRepository;
import com.cinebook.repository.ShowtimeSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowtimeSeatConcurrencyTest {

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private ShowtimeSeatRepository showtimeSeatRepository;

    private ShowtimeSeatService showtimeSeatService;

    @BeforeEach
    void setUp() {
        showtimeSeatService = new ShowtimeSeatService(showtimeRepository, showtimeSeatRepository, 5);
    }

    @Test
    void testConcurrentHoldRequestsOnSameSeatResultInOneSuccessAndOneConflict() throws Exception {
        Long showtimeId = 1L;
        Long seatId = 10L;

        Showtime showtime = new Showtime();
        showtime.setId(showtimeId);
        showtime.setStatus(ShowtimeStatus.SCHEDULED);

        Seat seat = new Seat();
        seat.setId(seatId);

        ShowtimeSeat ss = new ShowtimeSeat();
        ss.setId(100L);
        ss.setSeat(seat);
        ss.setStatus(ShowtimeSeatStatus.AVAILABLE);

        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));

        ReentrantLock dbPessimisticLock = new ReentrantLock();

        when(showtimeSeatRepository.findByShowtimeIdAndSeatIdInWithLock(eq(showtimeId), any()))
                .thenAnswer(invocation -> {
                    dbPessimisticLock.lock();
                    return List.of(ss);
                });

        when(showtimeSeatRepository.save(any(ShowtimeSeat.class)))
                .thenAnswer(invocation -> {
                    ShowtimeSeat saved = invocation.getArgument(0);
                    if (dbPessimisticLock.isHeldByCurrentThread()) {
                        dbPessimisticLock.unlock();
                    }
                    return saved;
                });

        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        Callable<Void> task = () -> {
            latch.await();
            try {
                showtimeSeatService.holdSeats(showtimeId, new HoldSeatsRequest(List.of(seatId)));
                successCount.incrementAndGet();
            } catch (SeatUnavailableException e) {
                if (dbPessimisticLock.isHeldByCurrentThread()) {
                    dbPessimisticLock.unlock();
                }
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                if (dbPessimisticLock.isHeldByCurrentThread()) {
                    dbPessimisticLock.unlock();
                }
                throw e;
            }
            return null;
        };

        Future<Void> future1 = executor.submit(task);
        Future<Void> future2 = executor.submit(task);

        latch.countDown(); // Start both threads simultaneously

        future1.get(5, TimeUnit.SECONDS);
        future2.get(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one concurrent request must succeed");
        assertEquals(1, conflictCount.get(), "Exactly one concurrent request must fail with conflict");
    }
}
