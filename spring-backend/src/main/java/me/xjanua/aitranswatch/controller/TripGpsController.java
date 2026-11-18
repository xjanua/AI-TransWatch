package me.xjanua.aitranswatch.controller;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import me.xjanua.aitranswatch.dto.RestResponse;
import me.xjanua.aitranswatch.dto.gps.GpsIngestionRequest;
import me.xjanua.aitranswatch.dto.gps.GpsLogResponse;
import me.xjanua.aitranswatch.service.GpsAlertService;

@RestController
@RequestMapping("/api/trips/{tripId}/gps")
public class TripGpsController {
    private final GpsAlertService service;
    public TripGpsController(GpsAlertService service) { this.service = service; }
    @PostMapping
    public RestResponse<GpsLogResponse> ingest(@PathVariable UUID tripId, @Valid @RequestBody GpsIngestionRequest request, Authentication authentication) {
        return RestResponse.success(service.ingest(tripId, request, authentication));
    }
    @GetMapping("/latest")
    public RestResponse<GpsLogResponse> latest(@PathVariable UUID tripId) {
        return RestResponse.success(service.latest(tripId));
    }
}
