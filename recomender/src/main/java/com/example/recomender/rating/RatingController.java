package com.example.recomender.rating;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.recomender.auth.AuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/ratings")
public class RatingController {

    private final RatingService ratingService;
    private final AuthorizationService authorizationService;

    public record RateMovieRequest(
            @NotNull Long movieId,
            @NotNull @DecimalMin("0.5") @DecimalMax("5.0") Double rating
    ) {}

    @PostMapping
    public ResponseEntity<Void> rateMovie(
            @PathVariable Long userId,
            @Valid @RequestBody RateMovieRequest req) {

        authorizationService.ensureCurrentUserOrAdmin(userId);
        ratingService.upsertRating(userId, req.movieId(), req.rating());
        return ResponseEntity.noContent().build();
    }
}