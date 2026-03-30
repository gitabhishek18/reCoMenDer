package com.example.recomender.reco;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ArtifactLoader {

    private final ObjectMapper objectMapper;

    private Map<Long, List<ScoredNeighbor>> cfSimilar = Collections.emptyMap();
    private Map<Long, List<ScoredNeighbor>> contentSimilar = Collections.emptyMap();
    private List<PopularItem> popular = List.of();
    private Map<Long, Double> popularScoreMap = Collections.emptyMap();

    @PostConstruct
    public void load() {
        try {
            ClassPathResource res = new ClassPathResource("artifacts/similar_items_cf.json");
            try (InputStream is = res.getInputStream()) {
                // JSON keys are strings, convert to Long keys
                Map<String, List<ScoredNeighbor>> raw =
                        objectMapper.readValue(is, new TypeReference<>() {});
                cfSimilar = raw.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                e -> Long.parseLong(e.getKey()),
                                Map.Entry::getValue
                        ));
            }
            System.out.println("Loaded CF similarity map size = " + cfSimilar.size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load CF artifact", e);
        }
        try {
            ClassPathResource res = new ClassPathResource("artifacts/similar_items_content.json");
            try (InputStream is = res.getInputStream()) {
                Map<String, List<ScoredNeighbor>> raw =
                        objectMapper.readValue(is, new TypeReference<>() {});
                contentSimilar = raw.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                e -> Long.parseLong(e.getKey()),
                                Map.Entry::getValue
                        ));
            }
            System.out.println("Loaded content similarity map size = " + contentSimilar.size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load content artifact", e);
        }


        try {
            ClassPathResource popRes = new ClassPathResource("artifacts/popular.json");
            try (InputStream ris = popRes.getInputStream()) {
                popular = objectMapper.readValue(ris, new TypeReference<>() {});
            }
            System.out.println("Loaded popular list size = " + popular.size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Popular artifact", e);
        }
        popularScoreMap = popular.stream()
            .collect(java.util.stream.Collectors.toMap(
                    PopularItem::movieId,
                    PopularItem::score
        ));
    }

    public List<ScoredNeighbor> getNeighbors(Long movieId) {
        return cfSimilar.getOrDefault(movieId, List.of());
    }
    public List<ScoredNeighbor> getContentNeighbors(Long movieId){
        return contentSimilar.getOrDefault(movieId, List.of());
    }
    public List<PopularItem> getPopular(int k) {
        if(popular.isEmpty())return List.of();
        k = Math.min(k, popular.size());
        return popular.subList(0, k);
    }
    public double getPopularityScore(Long movieId) {
        return popularScoreMap.getOrDefault(movieId, 0.0);
    }
}