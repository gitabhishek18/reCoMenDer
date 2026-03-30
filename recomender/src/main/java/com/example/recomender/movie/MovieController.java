package com.example.recomender.movie;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;



@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {
    private final MovieRepository movieRepository;

    @GetMapping
    public Page<Movie> searchMovies(
        @RequestParam(defaultValue = "") String query,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        if(page<0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"page must be>=0");
        if(size<=0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"size must be >0");
        size=Math.min(size, 50);
        PageRequest pageable = PageRequest.of(page, size);
        if(query == null || query.isBlank()){
            return Page.empty(pageable);
        }
        return movieRepository.findByTitleContainingIgnoreCase(query.trim(),pageable);
    }
    @GetMapping("/{id}")
    public Movie getMovie(@PathVariable Long id) {

        return movieRepository.findById(id)
        .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Movie Not found"));
    }
}
