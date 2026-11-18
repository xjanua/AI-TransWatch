package me.xjanua.aitranswatch.config;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.xjanua.aitranswatch.dto.RestResponse;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    @GetMapping
    public RestResponse<Map<String, Object>> health() {
        return RestResponse.success(Map.of("service", "spring-backend", "status", "UP", "timestamp", OffsetDateTime.now().toString()));
    }
}
