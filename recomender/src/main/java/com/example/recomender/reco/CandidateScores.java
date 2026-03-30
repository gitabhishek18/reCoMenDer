package com.example.recomender.reco;

import java.util.Map;
import java.util.Set;

public record CandidateScores(Map<Long, Double> scoreMap,
        Map<Long, Long> reasonFrom,
        Map<Long, Double> bestReasonContribution,
        Set<Long> seen) { 
}
