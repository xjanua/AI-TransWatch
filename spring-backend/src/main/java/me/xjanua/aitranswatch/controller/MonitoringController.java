package me.xjanua.aitranswatch.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.xjanua.aitranswatch.dto.RestResponse;
import me.xjanua.aitranswatch.dto.monitoring.LiveVehicleResponse;
import me.xjanua.aitranswatch.service.LiveMonitoringService;

/** API giám sát trực tuyến cho bản đồ đội xe. */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
    private final LiveMonitoringService liveMonitoringService;

    public MonitoringController(LiveMonitoringService liveMonitoringService) {
        this.liveMonitoringService = liveMonitoringService;
    }

    @GetMapping("/live")
    public RestResponse<List<LiveVehicleResponse>> liveFleet() {
        return RestResponse.success(liveMonitoringService.liveFleet());
    }
}
