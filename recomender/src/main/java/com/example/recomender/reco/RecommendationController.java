package com.example.recomender.reco;

import java.util.List;

// import org.springframework.security.access.AccessDeniedException;
// import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.recomender.auth.AuthorizationService;
// import com.example.recomender.user.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/recommendation")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final AuthorizationService authorizationService;
    @GetMapping
    public List<RecommendationItem> recommend(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int k,
            @RequestParam(defaultValue = "cf") String strategy) {

        authorizationService.ensureCurrentUserOrAdmin(userId);
        return recommendationService.recommend(userId, k, strategy);
    }
}