package me.xjanua.aitranswatch.dto.trip;

import jakarta.validation.constraints.NotNull;
import me.xjanua.aitranswatch.enums.TripStatus;

public record TripStatusRequest(@NotNull TripStatus status) {}
