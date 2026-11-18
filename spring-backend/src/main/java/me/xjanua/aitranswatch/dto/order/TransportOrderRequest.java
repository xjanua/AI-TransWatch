package me.xjanua.aitranswatch.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import me.xjanua.aitranswatch.enums.OrderPriority;
import me.xjanua.aitranswatch.enums.OrderStatus;

public record TransportOrderRequest(
        @NotBlank @Size(max = 50) String orderCode,
        @NotNull UUID customerId,
        OrderStatus status,
        OrderPriority priority,
        @NotBlank @Size(max = 255) String cargoName,
        @NotNull @DecimalMin(value = "0.01") BigDecimal weightKg,
        @NotBlank @Size(max = 500) String pickupAddress,
        BigDecimal pickupLatitude,
        BigDecimal pickupLongitude,
        @NotBlank @Size(max = 500) String deliveryAddress,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        LocalDateTime deliveryDeadline) {
}
