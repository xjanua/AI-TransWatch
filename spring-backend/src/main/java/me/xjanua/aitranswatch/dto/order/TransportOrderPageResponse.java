package me.xjanua.aitranswatch.dto.order;

import java.util.List;

public record TransportOrderPageResponse(
        List<TransportOrderResponse> content, int page, int size, long totalElements, int totalPages) {
}
