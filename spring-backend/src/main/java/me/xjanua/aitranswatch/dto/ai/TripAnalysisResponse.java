package me.xjanua.aitranswatch.dto.ai;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Public camelCase response returned by the Spring Boot API. */
public record TripAnalysisResponse(
        UUID tripId,
        BigDecimal etaMinutes,
        BigDecimal lateProbability,
        String riskLevel,
        BigDecimal anomalyScore,
        String modelVersion,
        boolean usingBaseline,
        List<String> explanations) {
}
