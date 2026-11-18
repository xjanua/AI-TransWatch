package me.xjanua.aitranswatch.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.xjanua.aitranswatch.dto.demo.DemoSimulationRequest;
import me.xjanua.aitranswatch.dto.demo.DemoSimulationResponse;
import me.xjanua.aitranswatch.dto.gps.GpsIngestionRequest;
import me.xjanua.aitranswatch.dto.gps.GpsLogResponse;

/**
 * Tạo một điểm GPS xác định cho mỗi lần gọi nhằm trình diễn rule engine.
 * Không có scheduler/background loop, vì vậy không phát sinh GPS giả ngoài thao tác
 * chủ động của ADMIN và an toàn cho môi trường phát triển/bảo vệ đồ án.
 */
@Service
@Transactional
public class DemoSimulatorService {
    private static final BigDecimal BASE_LATITUDE = new BigDecimal("10.8750000");
    private static final BigDecimal BASE_LONGITUDE = new BigDecimal("106.7900000");

    private final GpsAlertService gpsAlertService;

    public DemoSimulatorService(GpsAlertService gpsAlertService) {
        this.gpsAlertService = gpsAlertService;
    }

    public DemoSimulationResponse simulateStep(UUID tripId, DemoSimulationRequest.Scenario scenario) {
        if (scenario == DemoSimulationRequest.Scenario.CONTINUOUS_DRIVING) {
            // Cần hai mốc: điểm lùi 270 phút mở đầu ca lái và điểm hiện tại kết thúc ca lái,
            // nhờ vậy luật lái liên tục tính được quãng thời gian lái thực tế.
            gpsAlertService.ingestSimulation(tripId, scenarioRequest(scenario));
            GpsLogResponse latest = gpsAlertService.ingestSimulation(tripId, currentTelemetry());
            return new DemoSimulationResponse(scenario.name(),
                    "Đã tạo hai mốc telemetry (cách nhau 4,5 giờ, không có lần nghỉ hợp lệ); rule engine sẽ phát sinh CONTINUOUS_DRIVING/MEDIUM.",
                    latest, "CONTINUOUS_DRIVING/MEDIUM");
        }
        GpsIngestionRequest request = scenarioRequest(scenario);
        GpsLogResponse log = gpsAlertService.ingestSimulation(tripId, request);
        return switch (scenario) {
            case NORMAL -> new DemoSimulationResponse(scenario.name(),
                    "Đã tạo một điểm GPS bình thường; không phát sinh cảnh báo.", log, null);
            case SPEEDING -> new DemoSimulationResponse(scenario.name(),
                    "Đã tạo điểm GPS vượt ngưỡng 80 km/h; rule engine sẽ phát sinh SPEEDING/HIGH.", log, "SPEEDING/HIGH");
            case ABNORMAL_STOP -> new DemoSimulationResponse(scenario.name(),
                    "Đã tạo điểm GPS dừng 18 phút khi động cơ bật; rule engine sẽ phát sinh ABNORMAL_STOP/MEDIUM.", log, "ABNORMAL_STOP/MEDIUM");
            case ROUTE_DEVIATION -> new DemoSimulationResponse(scenario.name(),
                    "Đã tạo điểm GPS cách lộ trình dự kiến khoảng 3 km; rule engine sẽ phát sinh ROUTE_DEVIATION/HIGH.", log, "ROUTE_DEVIATION/HIGH");
            case CONTINUOUS_DRIVING -> throw new IllegalStateException("Đã xử lý ở nhánh riêng");
        };
    }

    private GpsIngestionRequest currentTelemetry() {
        return new GpsIngestionRequest(BASE_LATITUDE, BASE_LONGITUDE,
                new BigDecimal("52.00"), new BigDecimal("45.00"), true, LocalDateTime.now(), null);
    }

    private GpsIngestionRequest scenarioRequest(DemoSimulationRequest.Scenario scenario) {
        LocalDateTime now = LocalDateTime.now();
        return switch (scenario) {
            case NORMAL -> new GpsIngestionRequest(BASE_LATITUDE, BASE_LONGITUDE,
                    new BigDecimal("48.00"), new BigDecimal("45.00"), true, now, null);
            case SPEEDING -> new GpsIngestionRequest(BASE_LATITUDE.add(new BigDecimal("0.0030000")), BASE_LONGITUDE,
                    new BigDecimal("92.00"), new BigDecimal("50.00"), true, now, null);
            case ABNORMAL_STOP -> new GpsIngestionRequest(BASE_LATITUDE.add(new BigDecimal("0.0040000")), BASE_LONGITUDE,
                    BigDecimal.ZERO, new BigDecimal("50.00"), true, now, 18);
            // Lệch khoảng 0.03 độ vĩ tuyến (~3,3 km) so với lộ trình seed của TRIP-DEMO-001.
            case ROUTE_DEVIATION -> new GpsIngestionRequest(BASE_LATITUDE.add(new BigDecimal("0.0300000")), BASE_LONGITUDE,
                    new BigDecimal("55.00"), new BigDecimal("50.00"), true, now, null);
            // Điểm mở đầu ca lái, lùi 270 phút so với hiện tại.
            case CONTINUOUS_DRIVING -> new GpsIngestionRequest(BASE_LATITUDE, BASE_LONGITUDE,
                    new BigDecimal("52.00"), new BigDecimal("45.00"), true, now.minusMinutes(270), null);
        };
    }
}
