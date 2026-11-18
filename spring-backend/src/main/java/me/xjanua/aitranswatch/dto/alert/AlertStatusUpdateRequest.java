package me.xjanua.aitranswatch.dto.alert;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import me.xjanua.aitranswatch.enums.AlertStatus;

public record AlertStatusUpdateRequest(@NotNull AlertStatus status, @Size(max = 1000) String handlingNote) {}
