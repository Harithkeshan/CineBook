package com.cinebook.service;

import com.cinebook.dto.MovieCreateRequest;
import com.cinebook.dto.MovieUpdateRequest;
import com.cinebook.entity.Movie;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.MovieRepository;
import com.cinebook.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    public MovieService(MovieRepository movieRepository, ShowtimeRepository showtimeRepository) {
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public Movie getMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
    }

    @Transactional
    public Movie createMovie(MovieCreateRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Movie title is required");
        }

        Movie movie = new Movie();
        movie.setTitle(request.title().trim());
        movie.setDescription(request.description());
        movie.setDurationMinutes(request.durationMinutes());
        movie.setLanguage(request.language());
        movie.setGenre(request.genre());
        movie.setPosterUrl(request.posterUrl());
        movie.setTrailerUrl(request.trailerUrl());
        movie.setReleaseDate(request.releaseDate());
        movie.setAgeRating(request.ageRating());

        return movieRepository.save(movie);
    }

    @Transactional
    public Movie updateMovie(Long id, MovieUpdateRequest request) {
        Movie movie = getMovieById(id);

        if (request != null) {
            if (request.title() != null && !request.title().isBlank()) {
                movie.setTitle(request.title().trim());
            }
            movie.setDescription(request.description());
            movie.setDurationMinutes(request.durationMinutes());
            movie.setLanguage(request.language());
            movie.setGenre(request.genre());
            movie.setPosterUrl(request.posterUrl());
            movie.setTrailerUrl(request.trailerUrl());
            movie.setReleaseDate(request.releaseDate());
            movie.setAgeRating(request.ageRating());
        }

        return movieRepository.save(movie);
    }

    @Transactional
    public void deleteMovie(Long id) {
        Movie movie = getMovieById(id);
        if (showtimeRepository.existsByMovieId(id)) {
            throw new IllegalStateException("Cannot delete movie because it is referenced by existing showtimes");
        }
        movieRepository.delete(movie);
    }
}
