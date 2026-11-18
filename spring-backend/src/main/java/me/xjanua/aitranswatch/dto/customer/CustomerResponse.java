package me.xjanua.aitranswatch.dto.customer;

import java.time.LocalDateTime;
import java.util.UUID;

import me.xjanua.aitranswatch.enums.CustomerStatus;

public record CustomerResponse(
        UUID id, String code, String name, String contactName, String phone,
        String email, String address, CustomerStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
