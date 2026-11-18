package me.xjanua.aitranswatch.dto.ai;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;

/** Optional telemetry overrides; omitted values are sourced from the latest GPS log. */
public record TripAnalysisRequest(
        @DecimalMin("0.0") BigDecimal speedKmh,
        @DecimalMin("0.0") BigDecimal routeDeviationMeters,
        @DecimalMin("0.0") Integer stoppedMinutes,
        @DecimalMin("0.0") BigDecimal remainingDistanceKm) {
}
