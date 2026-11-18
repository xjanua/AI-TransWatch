package me.xjanua.aitranswatch.dto.monitoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import me.xjanua.aitranswatch.enums.TripStatus;

/** Vị trí mới nhất của một chuyến đang vận hành, phục vụ bản đồ giám sát. */
public record LiveVehicleResponse(
        UUID tripId,
        String tripCode,
        TripStatus status,
        UUID vehicleId,
        String vehiclePlate,
        String driverName,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal speedKmh,
        BigDecimal headingDegree,
        Boolean engineOn,
        LocalDateTime recordedAt) {
}
