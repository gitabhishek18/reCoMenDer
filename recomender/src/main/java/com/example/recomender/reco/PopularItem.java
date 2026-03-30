package com.example.recomender.reco;

public record PopularItem(
        Long movieId,
        String title,
        double score,
        double avgRating,
        long ratingCount,
        String reason
) {}