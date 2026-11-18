package me.xjanua.aitranswatch.dto.alert;

import java.time.LocalDateTime;
import java.util.UUID;
import me.xjanua.aitranswatch.enums.AlertSeverity;
import me.xjanua.aitranswatch.enums.AlertStatus;
import me.xjanua.aitranswatch.enums.AlertType;

public record AlertResponse(UUID id, UUID tripId, UUID vehicleId, UUID driverId, AlertType alertType,
                            AlertSeverity severity, AlertStatus status, String evidence, LocalDateTime occurredAt,
                            UUID handledBy, LocalDateTime handledAt, String handlingNote) {}
