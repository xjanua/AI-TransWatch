package me.xjanua.aitranswatch.dto.trip;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import me.xjanua.aitranswatch.enums.TripStatus;

public record TripResponse(UUID id, String tripCode, TripStatus status, UUID vehicleId, String vehiclePlate, UUID driverId, String driverName, LocalDateTime scheduledStartAt, LocalDateTime actualStartAt, LocalDateTime estimatedArrivalAt, LocalDateTime actualEndAt, List<TripOrderResponse> orders) {}
