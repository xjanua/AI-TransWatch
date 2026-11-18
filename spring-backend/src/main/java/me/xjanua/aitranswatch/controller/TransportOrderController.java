package me.xjanua.aitranswatch.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import me.xjanua.aitranswatch.dto.RestResponse;
import me.xjanua.aitranswatch.dto.order.TransportOrderPageResponse;
import me.xjanua.aitranswatch.dto.order.TransportOrderRequest;
import me.xjanua.aitranswatch.dto.order.TransportOrderResponse;
import me.xjanua.aitranswatch.enums.OrderStatus;
import me.xjanua.aitranswatch.service.TransportOrderService;

@RestController
@RequestMapping("/api/orders")
public class TransportOrderController {
    private final TransportOrderService transportOrderService;

    public TransportOrderController(TransportOrderService transportOrderService) {
        this.transportOrderService = transportOrderService;
    }

    @GetMapping
    public RestResponse<TransportOrderPageResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID customerId) {
        return RestResponse.success(transportOrderService.findAll(page, size, search, status, customerId));
    }

    @GetMapping("/{id}")
    public RestResponse<TransportOrderResponse> findById(@PathVariable UUID id) {
        return RestResponse.success(transportOrderService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DISPATCHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public RestResponse<TransportOrderResponse> create(@Valid @RequestBody TransportOrderRequest request) {
        return RestResponse.success(transportOrderService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DISPATCHER')")
    public RestResponse<TransportOrderResponse> update(@PathVariable UUID id, @Valid @RequestBody TransportOrderRequest request) {
        return RestResponse.success(transportOrderService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DISPATCHER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id) {
        transportOrderService.cancel(id);
    }
}
