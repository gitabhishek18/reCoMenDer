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
public class RecommendationService1 {

    private static final int MAX_K = 50;

    private final RatingRepository ratingRepository;
    private final MovieRepository movieRepository;
    private final ArtifactLoader artifactLoader;
    
    public List<RecommendationItem> recommend(Long userId, int k, String strategy) {
        k = Math.min(Math.max(k, 1), MAX_K);

        String s = (strategy == null) ? "cf" : strategy.trim().toLowerCase();
        if (s.isEmpty()) s = "cf";

        return switch (s) {
            case "popular" -> toPopularRecommendationItems(k);
            case "cf" -> recommendByNeighbors(userId, k, artifactLoader::getNeighbors);
            case "content" -> recommendByNeighbors(userId, k, artifactLoader::getContentNeighbors);
            case "hybrid"->recommendHybrid(userId, k);
            default -> throw new IllegalArgumentException("Unknown strategy: " + strategy);
        };
    }

    public List<RecommendationItem> recommendCf(Long userId, int k) {
        k = Math.min(Math.max(k, 1), MAX_K);
        return recommendByNeighbors(userId, k, artifactLoader::getNeighbors);
    }

    public List<RecommendationItem> recommendContent(Long userId, int k) {
        k = Math.min(Math.max(k, 1), MAX_K);
        return recommendByNeighbors(userId, k, artifactLoader::getContentNeighbors);
    }

    public List<RecommendationItem> recommendHybrid(Long userId, int k) {
        k = Math.min(Math.max(k, 1), MAX_K);

        List<Rating> ratings = ratingRepository.findByUser_IdOrderByRatedAtDesc(userId);
        if (ratings.isEmpty()) {
            return toPopularRecommendationItems(k);
        }

        int numRatings = ratings.size();
        double cfConfidence = computeCfUserConfidence(numRatings);
        double contentConfidence = 1.0 - cfConfidence;

        CandidateScores cfRaw = buildCandidateScores(userId, artifactLoader::getNeighbors);
        CandidateScores contentRaw = buildCandidateScores(userId, artifactLoader::getContentNeighbors);

        Map<Long, Double> cfNorm = normalizeScores(cfRaw.scoreMap());
        Map<Long, Double> contentNorm = normalizeScores(contentRaw.scoreMap());

        if (cfNorm.isEmpty() && contentNorm.isEmpty()) {
            return toPopularRecommendationItems(k);
        }

        Set<Long> allCandidates = new HashSet<>();
        allCandidates.addAll(cfNorm.keySet());
        allCandidates.addAll(contentNorm.keySet());

        Map<Long, Double> finalScoreMap = new HashMap<>();

        for (Long cand : allCandidates) {
            double cfScore = cfNorm.getOrDefault(cand, 0.0);
            double contentScore = contentNorm.getOrDefault(cand, 0.0);
            double popularityScore = normalizePopularity(cand);

            double personalizedScore =
                    (cfConfidence * cfScore) + (contentConfidence * contentScore);

            double finalScore =
                    (0.85 * personalizedScore) + (0.15 * popularityScore);

            if (finalScore > 0.0) {
                finalScoreMap.put(cand, finalScore);
            }
        }

        if (finalScoreMap.isEmpty()) {
            return toPopularRecommendationItems(k);
        }

        PriorityQueue<Map.Entry<Long, Double>> pq =
                new PriorityQueue<>(Map.Entry.comparingByValue());

        for (Map.Entry<Long, Double> e : finalScoreMap.entrySet()) {
            pq.offer(e);
            if (pq.size() > k) pq.poll();
        }

        if (pq.isEmpty()) {
            return toPopularRecommendationItems(k);
        }

        List<Long> topIds = new ArrayList<>(pq.size());
        while (!pq.isEmpty()) topIds.add(pq.poll().getKey());
        Collections.reverse(topIds);

        Set<Long> seen = new HashSet<>(cfRaw.seen());
        Map<Long, String> seenTitles = new HashMap<>();
        movieRepository.findAllById(seen).forEach(m -> seenTitles.put(m.getId(), m.getTitle()));

        Map<Long, Movie> movieMap = new HashMap<>();
        movieRepository.findAllById(topIds).forEach(m -> movieMap.put(m.getId(), m));

        List<RecommendationItem> out = new ArrayList<>(k);
        Set<Long> alreadyAdded = new HashSet<>(seen);

        for (Long mid : topIds) {
            Movie m = movieMap.get(mid);
            if (m == null) continue;

            // choose explanation source from stronger channel
            Long cfSrc = cfRaw.reasonFrom().get(mid);
            Long contentSrc = contentRaw.reasonFrom().get(mid);

            double cfContribution = cfRaw.bestReasonContribution().getOrDefault(mid, Double.NEGATIVE_INFINITY);
            double contentContribution = contentRaw.bestReasonContribution().getOrDefault(mid, Double.NEGATIVE_INFINITY);

            Long chosenSrc = null;
            if (cfContribution >= contentContribution) {
                chosenSrc = cfSrc;
            } else {
                chosenSrc = contentSrc;
            }

            String reason = "Hybrid recommendation";
            if (chosenSrc != null) {
                String srcTitle = seenTitles.getOrDefault(chosenSrc, "that movie");
                reason = "Because you liked " + srcTitle;
            }

            out.add(new RecommendationItem(
                    m.getId(),
                    m.getTitle(),
                    finalScoreMap.getOrDefault(mid, 0.0),
                    reason
            ));
            alreadyAdded.add(m.getId());
        }

        if (out.size() < k) {
            for (PopularItem p : artifactLoader.getPopular(k)) {
                if (out.size() >= k) break;
                if (alreadyAdded.contains(p.movieId())) continue;

                out.add(new RecommendationItem(
                        p.movieId(),
                        p.title(),
                        p.score(),
                        "Popular fallback"
                ));
                alreadyAdded.add(p.movieId());
            }
        }

        return out;
    }

    private CandidateScores buildCandidateScores(
            Long userId,
            java.util.function.Function<Long, List<ScoredNeighbor>> neighborProvider
    ) {
        List<Rating> ratings = ratingRepository.findByUser_IdOrderByRatedAtDesc(userId);

        if (ratings.isEmpty()) {
            return new CandidateScores(
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptySet()
            );
        }

        Set<Long> seen = new HashSet<>();
        for (Rating r : ratings) {
            seen.add(r.getMovie().getId());
        }

        Map<Long, Double> scoreMap = new HashMap<>();
        Map<Long, Long> reasonFrom = new HashMap<>();
        Map<Long, Double> bestReasonContribution = new HashMap<>();

        for (Rating r : ratings) {
            Long srcMovieId = r.getMovie().getId();
            double ratingValue = r.getRating().doubleValue();

            double preferenceWeight = (ratingValue - 3.0) / 2.0;

            if (preferenceWeight == 0.0) continue;

            List<ScoredNeighbor> neighbors = neighborProvider.apply(srcMovieId);
            if (neighbors.isEmpty()) continue;

            for (ScoredNeighbor nb : neighbors) {
                Long cand = nb.movieId();
                if (seen.contains(cand)) continue;

                double add = preferenceWeight * nb.score();
                scoreMap.merge(cand, add, Double::sum);

                
                if (add > 0) {
                    double prevBest = bestReasonContribution.getOrDefault(cand, Double.NEGATIVE_INFINITY);
                    if (add > prevBest) {
                        bestReasonContribution.put(cand, add);
                        reasonFrom.put(cand, srcMovieId);
                    }
                }
            }
        }

        return new CandidateScores(scoreMap, reasonFrom, bestReasonContribution, seen);
    }

    private Map<Long, Double> normalizeScores(Map<Long, Double> scores) {
        Map<Long, Double> normalized = new HashMap<>();

        double max = 0.0;
        for (double v : scores.values()) {
            if (v > max) max = v;
        }

        if (max <= 0.0) {
            return normalized;
        }

        for (Map.Entry<Long, Double> e : scores.entrySet()) {
            if (e.getValue() > 0.0) {
                normalized.put(e.getKey(), e.getValue() / max);
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
    
    private double computeCfUserConfidence(int numRatings){
        if(numRatings<5)return 0.30;
        if(numRatings<10)return 0.50;
        if(numRatings<20)return 0.70;
        return 0.85;
    }

    private List<RecommendationItem> recommendByNeighbors(
            Long userId,
            int k,
            Function<Long, List<ScoredNeighbor>> neighborProvider
    ) {
        List<Rating> ratings = ratingRepository.findByUser_IdOrderByRatedAtDesc(userId);

        if (ratings.isEmpty()) {
            return toPopularRecommendationItems(k);
        }

        Set<Long> seen = new HashSet<>();
        for (Rating r : ratings) {
            seen.add(r.getMovie().getId());
        }

        Map<Long, String> seenTitles = new HashMap<>();
        movieRepository.findAllById(seen).forEach(m -> seenTitles.put(m.getId(), m.getTitle()));

        Map<Long, Double> scoreMap = new HashMap<>();
        Map<Long, Long> reasonFrom = new HashMap<>();
        Map<Long, Double> bestReasonContribution = new HashMap<>();

        for (Rating r : ratings) {
            Long srcMovieId = r.getMovie().getId();
            double ratingValue = r.getRating().doubleValue();

            double preferenceWeight = (ratingValue - 3.0)/2.0;

            if (preferenceWeight == 0.0) continue;

            List<ScoredNeighbor> neighbors = neighborProvider.apply(srcMovieId);
            if (neighbors.isEmpty()) continue;

            for (ScoredNeighbor nb : neighbors) {
                Long cand = nb.movieId();
                if (seen.contains(cand)) continue;

                double add = preferenceWeight * nb.score();
                scoreMap.merge(cand, add, Double::sum);

                if (add > 0) {
                    double prevBest = bestReasonContribution.getOrDefault(cand, Double.NEGATIVE_INFINITY);
                    if (add > prevBest) {
                        bestReasonContribution.put(cand, add);
                        reasonFrom.put(cand, srcMovieId);
                    }
                }
            }
        }

        if (scoreMap.isEmpty()) {
            return toPopularRecommendationItems(k);
        }

        PriorityQueue<Map.Entry<Long, Double>> pq =
                new PriorityQueue<>(Map.Entry.comparingByValue());

        for (Map.Entry<Long, Double> e : scoreMap.entrySet()) {
            if (e.getValue() <= 0.0) continue;
            pq.offer(e);
            if (pq.size() > k) pq.poll();
        }
        if (pq.isEmpty()) {
            return toPopularRecommendationItems(k);
        }
        List<Long> topIds = new ArrayList<>(pq.size());
        while (!pq.isEmpty()) topIds.add(pq.poll().getKey());
        Collections.reverse(topIds);

        Map<Long, Movie> movieMap = new HashMap<>();
        movieRepository.findAllById(topIds).forEach(m -> movieMap.put(m.getId(), m));

        List<RecommendationItem> out = new ArrayList<>(k);
        Set<Long> alreadyAdded = new HashSet<>(seen);

        for (Long mid : topIds) {
            Movie m = movieMap.get(mid);
            if (m == null) continue;

            Long src = reasonFrom.get(mid);
            String reason = "Similar items";
            if (src != null) {
                String srcTitle = seenTitles.getOrDefault(src, "that movie");
                reason = "Because you liked " + srcTitle;
            }

            out.add(new RecommendationItem(
                    m.getId(),
                    m.getTitle(),
                    scoreMap.getOrDefault(mid, 0.0),
                    reason
            ));
            alreadyAdded.add(m.getId());
        }

        if (out.size() < k) {
            for (PopularItem p : artifactLoader.getPopular(k)) {
                if (out.size() >= k) break;
                if (alreadyAdded.contains(p.movieId())) continue;

                out.add(new RecommendationItem(
                        p.movieId(),
                        p.title(),
                        p.score(),
                        "Popular fallback"
                ));
                alreadyAdded.add(p.movieId());
            }
        }

        return out;
    }

    private List<RecommendationItem> toPopularRecommendationItems(int k) {
        return artifactLoader.getPopular(k).stream()
                .map(p -> new RecommendationItem(
                        p.movieId(),
                        p.title(),
                        p.score(),
                        "Popular fallback"
                ))
                .toList();
    }
}