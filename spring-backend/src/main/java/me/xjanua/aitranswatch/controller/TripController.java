package me.xjanua.aitranswatch.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import me.xjanua.aitranswatch.dto.RestResponse;
import me.xjanua.aitranswatch.dto.trip.TripCreateRequest;
import me.xjanua.aitranswatch.dto.trip.TripResponse;
import me.xjanua.aitranswatch.dto.trip.TripStatusRequest;
import me.xjanua.aitranswatch.enums.TripStatus;
import me.xjanua.aitranswatch.service.TripService;

@RestController
@RequestMapping("/api/trips")
public class TripController {
    private final TripService tripService;
    public TripController(TripService tripService) { this.tripService = tripService; }
    @GetMapping
    public RestResponse<Page<TripResponse>> findAll(@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size, @RequestParam(required = false) TripStatus status, @RequestParam(required = false) UUID vehicleId, @RequestParam(required = false) UUID driverId) {
        return RestResponse.success(tripService.findAll(page, size, status, vehicleId, driverId));
    }
    @GetMapping("/{id}")
    public RestResponse<TripResponse> findById(@PathVariable UUID id) { return RestResponse.success(tripService.findById(id)); }
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DISPATCHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public RestResponse<TripResponse> create(@Valid @RequestBody TripCreateRequest request) { return RestResponse.success(tripService.create(request)); }
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DISPATCHER')")
    public RestResponse<TripResponse> transition(@PathVariable UUID id, @Valid @RequestBody TripStatusRequest request) { return RestResponse.success(tripService.transition(id, request.status())); }
}
