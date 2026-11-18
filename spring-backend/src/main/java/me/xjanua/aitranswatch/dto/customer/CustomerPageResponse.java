package me.xjanua.aitranswatch.dto.customer;

import java.util.List;

public record CustomerPageResponse(
        List<CustomerResponse> items, int page, int size, long totalItems, int totalPages) {
}
