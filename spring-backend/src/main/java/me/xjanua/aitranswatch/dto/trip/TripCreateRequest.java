package me.xjanua.aitranswatch.dto.trip;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TripCreateRequest(
    @NotBlank @Size(max = 50) String tripCode,
    @NotNull UUID vehicleId,
    @NotNull UUID driverId,
    @NotEmpty List<@NotNull UUID> orderIds,
    @NotNull @FutureOrPresent LocalDateTime scheduledStartAt,
    LocalDateTime estimatedArrivalAt
) {}
