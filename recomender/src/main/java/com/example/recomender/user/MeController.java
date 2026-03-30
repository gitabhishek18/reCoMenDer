package com.example.recomender.user;

import com.example.recomender.auth.AuthenticatedUserService;
import com.example.recomender.rating.MyRatingResponse;
import com.example.recomender.rating.RatingRequest;
import com.example.recomender.rating.RatingService;
import com.example.recomender.reco.RecommendationItem;
import com.example.recomender.reco.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final AuthenticatedUserService authenticatedUserService;
    private final RecommendationService recommendationService;
    private final RatingService ratingService;

    @GetMapping("/profile")
    public MeProfileResponse getMyProfile() {
        User user = authenticatedUserService.getCurrentUser();
        return new MeProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
    @GetMapping("/recommendations")
    public List<RecommendationItem> getMyRecommendations(
            @RequestParam(defaultValue = "20") int k,
            @RequestParam(defaultValue = "cf") String strategy) {

        Long userId = authenticatedUserService.getCurrentUserId();
        return recommendationService.recommend(userId, k, strategy);
    }

    @PostMapping("/ratings")
    public ResponseEntity<Void> rateMovie(@Valid @RequestBody RatingRequest request) {
        Long userId = authenticatedUserService.getCurrentUserId();
        ratingService.upsertRating(userId, request.getMovieId(), request.getRating());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ratings")
    public List<MyRatingResponse> allRatings() {
        Long userId = authenticatedUserService.getCurrentUserId();
        return ratingService.getRatingResponsesForUser(userId);
    }
}