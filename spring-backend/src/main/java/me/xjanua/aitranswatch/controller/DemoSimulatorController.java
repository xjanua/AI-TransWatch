package me.xjanua.aitranswatch.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import me.xjanua.aitranswatch.dto.RestResponse;
import me.xjanua.aitranswatch.dto.demo.DemoSimulationRequest;
import me.xjanua.aitranswatch.dto.demo.DemoSimulationResponse;
import me.xjanua.aitranswatch.service.DemoSimulatorService;
import me.xjanua.aitranswatch.service.FleetAlertScheduler;

@RestController
@Profile({"dev", "demo"})
@RequestMapping("/api/demo/trips")
public class DemoSimulatorController {
    private final DemoSimulatorService demoSimulatorService;
    private final FleetAlertScheduler fleetAlertScheduler;

    public DemoSimulatorController(DemoSimulatorService demoSimulatorService, FleetAlertScheduler fleetAlertScheduler) {
        this.demoSimulatorService = demoSimulatorService;
        this.fleetAlertScheduler = fleetAlertScheduler;
    }

    /** Chỉ ADMIN được kích hoạt dữ liệu GPS mô phỏng trong demo. */
    @PostMapping("/{tripId}/simulate-step")
    @PreAuthorize("hasRole('ADMIN')")
    public RestResponse<DemoSimulationResponse> simulateStep(
            @PathVariable UUID tripId,
            @Valid @RequestBody DemoSimulationRequest request) {
        return RestResponse.success(demoSimulatorService.simulateStep(tripId, request.scenario()));
    }

    /**
     * Chạy ngay các kiểm tra cảnh báo định kỳ (mất GPS, giấy tờ sắp hết hạn, đến hạn bảo dưỡng)
     * để trình diễn và kiểm thử mà không phải chờ lịch tác vụ.
     */
    @PostMapping("/alerts/run-checks")
    @PreAuthorize("hasRole('ADMIN')")
    public RestResponse<Map<String, Object>> runAlertChecks() {
        int created = fleetAlertScheduler.runChecks();
        return RestResponse.success(Map.of("createdAlerts", created));
    }
}
