package com.cinebook.repository;

import com.cinebook.entity.ShowtimeSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {
    List<ShowtimeSeat> findByShowtimeId(Long showtimeId);
    Optional<ShowtimeSeat> findByShowtimeIdAndSeatId(Long showtimeId, Long seatId);
    boolean existsBySeatId(Long seatId);
    boolean existsByShowtimeId(Long showtimeId);

    @Query("SELECT ss FROM ShowtimeSeat ss " +
           "JOIN FETCH ss.seat s " +
           "JOIN FETCH s.section sec " +
           "WHERE ss.showtime.id = :showtimeId " +
           "ORDER BY sec.id ASC, s.rowLabel ASC, s.seatNumber ASC")
    List<ShowtimeSeat> findByShowtimeIdWithSeatAndSectionOrdered(@Param("showtimeId") Long showtimeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowtimeSeat ss " +
           "JOIN FETCH ss.seat s " +
           "WHERE ss.showtime.id = :showtimeId AND ss.seat.id IN :seatIds")
    List<ShowtimeSeat> findByShowtimeIdAndSeatIdInWithLock(@Param("showtimeId") Long showtimeId, @Param("seatIds") List<Long> seatIds);
}
