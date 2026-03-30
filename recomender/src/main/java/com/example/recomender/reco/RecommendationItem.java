package com.example.recomender.reco;

public record RecommendationItem(Long movieId, String title, double score, String reason) {}
