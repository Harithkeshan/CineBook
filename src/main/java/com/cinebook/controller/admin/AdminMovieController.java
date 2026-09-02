package com.cinebook.controller.admin;

import com.cinebook.dto.MovieCreateRequest;
import com.cinebook.dto.MovieResponse;
import com.cinebook.dto.MovieUpdateRequest;
import com.cinebook.entity.Movie;
import com.cinebook.service.MovieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/movies")
public class AdminMovieController {

    private final MovieService movieService;

    public AdminMovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@RequestBody MovieCreateRequest request) {
        Movie movie = movieService.createMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(movie));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovieResponse> updateMovie(@PathVariable Long id, @RequestBody MovieUpdateRequest request) {
        Movie movie = movieService.updateMovie(id, request);
        return ResponseEntity.ok(mapToResponse(movie));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }

    private MovieResponse mapToResponse(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getDurationMinutes(),
                movie.getLanguage(),
                movie.getGenre(),
                movie.getPosterUrl(),
                movie.getTrailerUrl(),
                movie.getReleaseDate(),
                movie.getAgeRating()
        );
    }
}
