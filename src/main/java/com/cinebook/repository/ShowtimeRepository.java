package com.cinebook.repository;

import com.cinebook.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByMovieId(Long movieId);
    List<Showtime> findByHallId(Long hallId);
    List<Showtime> findByMovieIdAndHallId(Long movieId, Long hallId);
}
