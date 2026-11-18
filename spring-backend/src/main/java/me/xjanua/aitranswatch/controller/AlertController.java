package me.xjanua.aitranswatch.controller;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import me.xjanua.aitranswatch.dto.RestResponse;
import me.xjanua.aitranswatch.dto.alert.AlertResponse;
import me.xjanua.aitranswatch.dto.alert.AlertStatusUpdateRequest;
import me.xjanua.aitranswatch.enums.AlertSeverity;
import me.xjanua.aitranswatch.enums.AlertStatus;
import me.xjanua.aitranswatch.service.GpsAlertService;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final GpsAlertService service;
    public AlertController(GpsAlertService service) { this.service = service; }
    @GetMapping
    public RestResponse<Page<AlertResponse>> list(@RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSeverity severity, @RequestParam(required = false) UUID tripId,
            @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return RestResponse.success(service.list(status, severity, tripId, PageRequest.of(page, size, Sort.by("occurredAt").descending())));
    }
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public RestResponse<AlertResponse> updateStatus(@PathVariable UUID id, @Valid @RequestBody AlertStatusUpdateRequest request, Authentication authentication) {
        return RestResponse.success(service.updateStatus(id, request, authentication));
    }
}
