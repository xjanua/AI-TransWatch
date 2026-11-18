package me.xjanua.aitranswatch.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import me.xjanua.aitranswatch.enums.OrderPriority;
import me.xjanua.aitranswatch.enums.OrderStatus;

public record TransportOrderResponse(
        UUID id, String orderCode, UUID customerId, String customerCode, String customerName,
        OrderStatus status, OrderPriority priority, String cargoName, BigDecimal weightKg,
        String pickupAddress, BigDecimal pickupLatitude, BigDecimal pickupLongitude,
        String deliveryAddress, BigDecimal deliveryLatitude, BigDecimal deliveryLongitude,
        LocalDateTime deliveryDeadline, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
