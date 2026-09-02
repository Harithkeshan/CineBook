package com.cinebook.repository;

import com.cinebook.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByMovieId(Long movieId);
    List<Showtime> findByHallId(Long hallId);
    List<Showtime> findByMovieIdAndHallId(Long movieId, Long hallId);
    boolean existsByMovieId(Long movieId);
    boolean existsByHallId(Long hallId);

    @Query("SELECT s FROM Showtime s WHERE s.hall.id = :hallId AND s.status != 'CANCELLED' AND s.startTime < :endTime AND s.endTime > :startTime")
    List<Showtime> findOverlappingShowtimes(
            @Param("hallId") Long hallId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    @Query("SELECT s FROM Showtime s WHERE s.hall.id = :hallId AND s.id != :excludeId AND s.status != 'CANCELLED' AND s.startTime < :endTime AND s.endTime > :startTime")
    List<Showtime> findOverlappingShowtimesExcludingId(
            @Param("hallId") Long hallId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("excludeId") Long excludeId
    );
}
