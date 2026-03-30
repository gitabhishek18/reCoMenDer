package com.example.recomender.reco;

import com.example.recomender.movie.Movie;
import com.example.recomender.movie.MovieRepository;
import com.example.recomender.rating.Rating;
import com.example.recomender.rating.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int MAX_K = 50;

    // Hybrid scoring weights
    private static final double HYBRID_PERSONALIZED_WEIGHT = 0.85;
    private static final double HYBRID_POPULARITY_WEIGHT = 0.15;

    // MMR settings
    private static final double MMR_LAMBDA = 0.75;

    private final RatingRepository ratingRepository;
    private final MovieRepository movieRepository;
    private final ArtifactLoader artifactLoader;

    public List<RecommendationItem> recommend(Long userId, int k, String strategy) {
        k = clampK(k);

        String s = (strategy == null) ? "cf" : strategy.trim().toLowerCase();
        if (s.isEmpty()) s = "cf";

        return switch (s) {
            case "popular" -> toPopularRecommendationItems(k);
            case "cf" -> recommendSingleStrategy(userId, k, artifactLoader::getNeighbors, "Similar items");
            case "content" -> recommendSingleStrategy(userId, k, artifactLoader::getContentNeighbors, "Similar items");
            case "hybrid" -> recommendHybrid(userId, k);
            default -> throw new IllegalArgumentException("Unknown strategy: " + strategy);
        };
    }

    public List<RecommendationItem> recommendCf(Long userId, int k) {
        return recommendSingleStrategy(userId, clampK(k), artifactLoader::getNeighbors, "Similar items");
    }

    public List<RecommendationItem> recommendContent(Long userId, int k) {
        return recommendSingleStrategy(userId, clampK(k), artifactLoader::getContentNeighbors, "Similar items");
    }

    public List<RecommendationItem> recommendHybrid(Long userId, int k) {
        k = clampK(k);

        List<Rating> ratings = ratingRepository.findByUser_IdOrderByRatedAtDesc(userId);
        if (ratings.isEmpty()) {
            return toPopularRecommendationItems(k);
        }

        Set<Long> seen = getSeenMovieIds(ratings);
        Map<Long, String> seenTitles = getMovieTitles(seen);

        CandidateScores cfRaw = buildCandidateScores(ratings, artifactLoader::getNeighbors);
        CandidateScores contentRaw = buildCandidateScores(ratings, artifactLoader::getContentNeighbors);

        Map<Long, Double> cfNorm = normalizeScores(cfRaw.scoreMap());
        Map<Long, Double> contentNorm = normalizeScores(contentRaw.scoreMap());

        if (cfNorm.isEmpty() && contentNorm.isEmpty()) {
            return toPopularRecommendationItems(k);
        }

        int numRatings = ratings.size();
        double cfConfidence = computeCfUserConfidence(numRatings);
        double contentConfidence = 1.0 - cfConfidence;

        Set<Long> allCandidates = new HashSet<>();
        allCandidates.addAll(cfNorm.keySet());
        allCandidates.addAll(contentNorm.keySet());

        Map<Long, Double> finalScoreMap = new HashMap<>();
        Map<Long, Long> finalReasonFrom = new HashMap<>();

        for (Long cand : allCandidates) {
            double cfScore = cfNorm.getOrDefault(cand, 0.0);
            double contentScore = contentNorm.getOrDefault(cand, 0.0);
            double popularityScore = normalizePopularity(cand);

            double personalizedScore =
                    (cfConfidence * cfScore) + (contentConfidence * contentScore);

            double finalScore =
                    (HYBRID_PERSONALIZED_WEIGHT * personalizedScore) +
                    (HYBRID_POPULARITY_WEIGHT * popularityScore);

            if (finalScore <= 0.0) continue;
            finalScoreMap.put(cand, finalScore);

            Long reasonSource = chooseReasonSource(cand, cfRaw, contentRaw);
            if (reasonSource != null) {
                finalReasonFrom.put(cand, reasonSource);
            }
        }

        if (finalScoreMap.isEmpty()) {
            return toPopularRecommendationItems(k);
        }

        int poolSize = Math.max(50, 3 * k);
        List<Long> topIds = applyMMR(finalScoreMap, k, poolSize, MMR_LAMBDA);

        if (topIds.isEmpty()) {
            return toPopularRecommendationItems(k);
        }

        return buildRecommendationsFromOrderedIds(
                topIds,
                k,
                seen,
                seenTitles,
                finalScoreMap,
                finalReasonFrom,
                "Hybrid recommendation"
        );
    }

    private List<RecommendationItem> recommendSingleStrategy(
            Long userId,
            int k,
            Function<Long, List<ScoredNeighbor>> neighborProvider,
            String defaultReason
    ) {
        List<Rating> ratings = ratingRepository.findByUser_IdOrderByRatedAtDesc(userId);
        if (ratings.isEmpty()) {
            return toPopularRecommendationItems(k);
        }

        Set<Long> seen = getSeenMovieIds(ratings);
        Map<Long, String> seenTitles = getMovieTitles(seen);

        CandidateScores raw = buildCandidateScores(ratings, neighborProvider);

        List<Long> topIds = selectTopPositiveCandidateIds(raw.scoreMap(), k);
        if (topIds.isEmpty()) {
            return toPopularRecommendationItems(k);
        }

        return buildRecommendationsFromOrderedIds(
                topIds,
                k,
                seen,
                seenTitles,
                raw.scoreMap(),
                raw.reasonFrom(),
                defaultReason
        );
    }

    private CandidateScores buildCandidateScores(
            List<Rating> ratings,
            Function<Long, List<ScoredNeighbor>> neighborProvider
    ) {
        if (ratings.isEmpty()) {
            return new CandidateScores(
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptySet()
            );
        }

        Set<Long> seen = getSeenMovieIds(ratings);

        Map<Long, Double> scoreMap = new HashMap<>();
        Map<Long, Long> reasonFrom = new HashMap<>();
        Map<Long, Double> bestReasonContribution = new HashMap<>();

        for (Rating rating : ratings) {
            Long sourceMovieId = rating.getMovie().getId();
            double ratingValue = rating.getRating().doubleValue();

            // 1,2 negative | 3 neutral | 4,5 positive
            double preferenceWeight = (ratingValue - 3.0) / 2.0;
            if (preferenceWeight == 0.0) continue;

            List<ScoredNeighbor> neighbors = neighborProvider.apply(sourceMovieId);
            if (neighbors.isEmpty()) continue;

            for (ScoredNeighbor nb : neighbors) {
                Long candidateMovieId = nb.movieId();
                if (seen.contains(candidateMovieId)) continue;

                double contribution = preferenceWeight * nb.score();
                scoreMap.merge(candidateMovieId, contribution, Double::sum);

                if (contribution > 0.0) {
                    double prevBest =
                            bestReasonContribution.getOrDefault(candidateMovieId, Double.NEGATIVE_INFINITY);
                    if (contribution > prevBest) {
                        bestReasonContribution.put(candidateMovieId, contribution);
                        reasonFrom.put(candidateMovieId, sourceMovieId);
                    }
                }
            }
        }

        return new CandidateScores(scoreMap, reasonFrom, bestReasonContribution, seen);
    }

    private List<Long> applyMMR(
            Map<Long, Double> relevanceScores,
            int finalK,
            int poolSize,
            double lambda
    ) {
        if (relevanceScores.isEmpty() || finalK <= 0) {
            return List.of();
        }

        PriorityQueue<Map.Entry<Long, Double>> pq =
                new PriorityQueue<>(Map.Entry.comparingByValue());

        for (Map.Entry<Long, Double> entry : relevanceScores.entrySet()) {
            if (entry.getValue() <= 0.0) continue;
            pq.offer(entry);
            if (pq.size() > poolSize) pq.poll();
        }

        if (pq.isEmpty()) {
            return List.of();
        }

        List<Long> candidatePool = new ArrayList<>(pq.size());
        while (!pq.isEmpty()) {
            candidatePool.add(pq.poll().getKey());
        }
        Collections.reverse(candidatePool);

        List<Long> selected = new ArrayList<>();
        Set<Long> remaining = new HashSet<>(candidatePool);

        while (!remaining.isEmpty() && selected.size() < finalK) {
            Long bestCandidate = null;
            double bestMmrScore = Double.NEGATIVE_INFINITY;

            for (Long candidate : remaining) {
                double relevance = relevanceScores.getOrDefault(candidate, 0.0);

                double maxSimilarityToSelected = 0.0;
                for (Long chosen : selected) {
                    double sim = getContentSimilarity(candidate, chosen);
                    if (sim > maxSimilarityToSelected) {
                        maxSimilarityToSelected = sim;
                    }
                }

                double mmrScore = (lambda * relevance) - ((1.0 - lambda) * maxSimilarityToSelected);

                if (mmrScore > bestMmrScore) {
                    bestMmrScore = mmrScore;
                    bestCandidate = candidate;
                }
            }

            if (bestCandidate == null) {
                break;
            }

            selected.add(bestCandidate);
            remaining.remove(bestCandidate);
        }

        return selected;
    }

    private double getContentSimilarity(Long movieA, Long movieB) {
        if (movieA == null || movieB == null) return 0.0;
        if (movieA.equals(movieB)) return 1.0;

        for (ScoredNeighbor nb : artifactLoader.getContentNeighbors(movieA)) {
            if (movieB.equals(nb.movieId())) {
                return nb.score();
            }
        }

        for (ScoredNeighbor nb : artifactLoader.getContentNeighbors(movieB)) {
            if (movieA.equals(nb.movieId())) {
                return nb.score();
            }
        }

        return 0.0;
    }

    private List<RecommendationItem> buildRecommendationsFromOrderedIds(
            List<Long> orderedIds,
            int k,
            Set<Long> seen,
            Map<Long, String> seenTitles,
            Map<Long, Double> scoreMap,
            Map<Long, Long> reasonFrom,
            String defaultReason
    ) {
        Map<Long, Movie> movieMap = getMoviesByIds(orderedIds);

        List<RecommendationItem> out = new ArrayList<>(k);
        Set<Long> alreadyAdded = new HashSet<>(seen);

        for (Long movieId : orderedIds) {
            Movie movie = movieMap.get(movieId);
            if (movie == null) continue;

            String reason = buildReason(movieId, reasonFrom, seenTitles, defaultReason);

            out.add(new RecommendationItem(
                    movie.getId(),
                    movie.getTitle(),
                    scoreMap.getOrDefault(movieId, 0.0),
                    reason
            ));
            alreadyAdded.add(movie.getId());
        }

        fillWithPopularFallback(out, alreadyAdded, k);
        return out;
    }

    private List<Long> selectTopPositiveCandidateIds(Map<Long, Double> scoreMap, int k) {
        if (scoreMap.isEmpty()) return List.of();

        PriorityQueue<Map.Entry<Long, Double>> pq =
                new PriorityQueue<>(Map.Entry.comparingByValue());

        for (Map.Entry<Long, Double> entry : scoreMap.entrySet()) {
            if (entry.getValue() <= 0.0) continue;
            pq.offer(entry);
            if (pq.size() > k) pq.poll();
        }

        if (pq.isEmpty()) return List.of();

        List<Long> topIds = new ArrayList<>(pq.size());
        while (!pq.isEmpty()) {
            topIds.add(pq.poll().getKey());
        }
        Collections.reverse(topIds);
        return topIds;
    }

    private void fillWithPopularFallback(
            List<RecommendationItem> out,
            Set<Long> alreadyAdded,
            int k
    ) {
        if (out.size() >= k) return;

        for (PopularItem popularItem : artifactLoader.getPopular(k)) {
            if (out.size() >= k) break;
            if (alreadyAdded.contains(popularItem.movieId())) continue;

            out.add(new RecommendationItem(
                    popularItem.movieId(),
                    popularItem.title(),
                    popularItem.score(),
                    "Popular fallback"
            ));
            alreadyAdded.add(popularItem.movieId());
        }
    }

    private String buildReason(
            Long candidateMovieId,
            Map<Long, Long> reasonFrom,
            Map<Long, String> seenTitles,
            String defaultReason
    ) {
        Long sourceMovieId = reasonFrom.get(candidateMovieId);
        if (sourceMovieId == null) return defaultReason;

        String sourceTitle = seenTitles.getOrDefault(sourceMovieId, "that movie");
        return "Because you liked " + sourceTitle;
    }

    private Long chooseReasonSource(
            Long candidateMovieId,
            CandidateScores cfRaw,
            CandidateScores contentRaw
    ) {
        double cfContribution =
                cfRaw.bestReasonContribution().getOrDefault(candidateMovieId, Double.NEGATIVE_INFINITY);
        double contentContribution =
                contentRaw.bestReasonContribution().getOrDefault(candidateMovieId, Double.NEGATIVE_INFINITY);

        if (cfContribution >= contentContribution) {
            return cfRaw.reasonFrom().get(candidateMovieId);
        }
        return contentRaw.reasonFrom().get(candidateMovieId);
    }

    private Map<Long, Double> normalizeScores(Map<Long, Double> scores) {
        if (scores.isEmpty()) return Collections.emptyMap();

        double maxPositive = 0.0;
        for (double value : scores.values()) {
            if (value > maxPositive) maxPositive = value;
        }

        if (maxPositive <= 0.0) {
            return Collections.emptyMap();
        }

        Map<Long, Double> normalized = new HashMap<>();
        for (Map.Entry<Long, Double> entry : scores.entrySet()) {
            if (entry.getValue() > 0.0) {
                normalized.put(entry.getKey(), entry.getValue() / maxPositive);
            }
        }
        return normalized;
    }

    private double normalizePopularity(Long movieId) {
        List<PopularItem> top = artifactLoader.getPopular(1);
        if (top.isEmpty()) return 0.0;

        double maxPopular = top.get(0).score();
        if (maxPopular <= 0.0) return 0.0;

        return artifactLoader.getPopularityScore(movieId) / maxPopular;
    }

    private double computeCfUserConfidence(int numRatings) {
        if (numRatings < 5) return 0.30;
        if (numRatings < 10) return 0.50;
        if (numRatings < 20) return 0.70;
        return 0.85;
    }

    private int clampK(int k) {
        return Math.min(Math.max(k, 1), MAX_K);
    }

    private Set<Long> getSeenMovieIds(List<Rating> ratings) {
        Set<Long> seen = new HashSet<>();
        for (Rating rating : ratings) {
            seen.add(rating.getMovie().getId());
        }
        return seen;
    }

    private Map<Long, String> getMovieTitles(Set<Long> movieIds) {
        if (movieIds.isEmpty()) return Collections.emptyMap();

        Map<Long, String> titles = new HashMap<>();
        movieRepository.findAllById(movieIds).forEach(movie -> titles.put(movie.getId(), movie.getTitle()));
        return titles;
    }

    private Map<Long, Movie> getMoviesByIds(List<Long> movieIds) {
        if (movieIds.isEmpty()) return Collections.emptyMap();

        Map<Long, Movie> movieMap = new HashMap<>();
        movieRepository.findAllById(movieIds).forEach(movie -> movieMap.put(movie.getId(), movie));
        return movieMap;
    }

    private List<RecommendationItem> toPopularRecommendationItems(int k) {
        return artifactLoader.getPopular(k).stream()
                .map(popularItem -> new RecommendationItem(
                        popularItem.movieId(),
                        popularItem.title(),
                        popularItem.score(),
                        "Popular fallback"
                ))
                .toList();
    }
}