package me.xjanua.aitranswatch.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import me.xjanua.aitranswatch.dto.RestResponse;
import me.xjanua.aitranswatch.dto.ai.TripAnalysisRequest;
import me.xjanua.aitranswatch.dto.ai.TripAnalysisResponse;
import me.xjanua.aitranswatch.service.AiPredictionService;

@RestController
@RequestMapping("/api/trips/{tripId}/analysis")
public class TripAnalysisController {
    private final AiPredictionService aiPredictionService;

    public TripAnalysisController(AiPredictionService aiPredictionService) {
        this.aiPredictionService = aiPredictionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DISPATCHER')")
    public RestResponse<TripAnalysisResponse> analyze(@PathVariable UUID tripId,
            @Valid @RequestBody(required = false) TripAnalysisRequest request) {
        TripAnalysisRequest payload = request == null ? new TripAnalysisRequest(null, null, null, null) : request;
        return RestResponse.success(aiPredictionService.analyze(tripId, payload));
    }
}
