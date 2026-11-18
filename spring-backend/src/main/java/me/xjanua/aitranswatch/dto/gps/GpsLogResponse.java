package me.xjanua.aitranswatch.dto.gps;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record GpsLogResponse(Long id, UUID tripId, UUID vehicleId, BigDecimal latitude, BigDecimal longitude,
                             BigDecimal speedKmh, BigDecimal headingDegree, boolean engineOn, LocalDateTime recordedAt) {}
