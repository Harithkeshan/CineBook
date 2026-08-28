package com.cinebook.service;

import com.cinebook.entity.Showtime;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;

    public ShowtimeService(ShowtimeRepository showtimeRepository) {
        this.showtimeRepository = showtimeRepository;
    }

    public List<Showtime> getAllShowtimes() {
        return showtimeRepository.findAll();
    }

    public List<Showtime> getShowtimesByMovieId(Long movieId) {
        return showtimeRepository.findByMovieId(movieId);
    }

    public List<Showtime> getShowtimesByHallId(Long hallId) {
        return showtimeRepository.findByHallId(hallId);
    }

    public List<Showtime> getShowtimesByMovieIdAndHallId(Long movieId, Long hallId) {
        return showtimeRepository.findByMovieIdAndHallId(movieId, hallId);
    }

    public Showtime getShowtimeById(Long id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + id));
    }
}
