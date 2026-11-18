package me.xjanua.aitranswatch.dto.gps;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

public record GpsIngestionRequest(
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
    @NotNull @DecimalMin("0.0") @DecimalMax("300.0") BigDecimal speedKmh,
    @DecimalMin("0.0") @DecimalMax("360.0") BigDecimal headingDegree,
    @NotNull Boolean engineOn,
    @PastOrPresent LocalDateTime recordedAt,
    @DecimalMin("0.0") Integer stoppedMinutes
) {}
