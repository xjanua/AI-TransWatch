package me.xjanua.aitranswatch.dto.trip;

import java.math.BigDecimal;
import java.util.UUID;
import me.xjanua.aitranswatch.enums.OrderStatus;

public record TripOrderResponse(UUID orderId, String orderCode, String cargoName, BigDecimal weightKg, OrderStatus status, int sequenceNo) {}
