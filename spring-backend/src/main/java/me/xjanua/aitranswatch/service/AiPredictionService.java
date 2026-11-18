package me.xjanua.aitranswatch.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.xjanua.aitranswatch.dto.ai.TripAnalysisRequest;
import me.xjanua.aitranswatch.dto.ai.TripAnalysisResponse;
import me.xjanua.aitranswatch.model.GpsLog;
import me.xjanua.aitranswatch.model.Trip;
import me.xjanua.aitranswatch.model.TripOrder;
import me.xjanua.aitranswatch.repository.GpsLogRepository;
import me.xjanua.aitranswatch.repository.TripOrderRepository;
import me.xjanua.aitranswatch.repository.TripRepository;

/**
 * Cầu nối Spring Boot → FastAPI.
 *
 * <p>Hợp đồng dây của FastAPI dùng snake_case: {@code speed_kmh, route_deviation_meters,
 * stopped_minutes, remaining_distance_km, hour_of_day, load_percent,
 * historical_avg_speed_kmh}; phản hồi trả {@code eta_minutes, late_probability,
 * risk_level, anomaly_score, model_version, using_baseline, explanations}.
 *
 * <p>HTTP được gọi trực tiếp bằng {@link HttpClient} của JDK kèm timeout tường minh và
 * JSON ghi/đọc bằng {@link ObjectMapper} của ứng dụng. Cách này tránh phụ thuộc vào
 * việc Spring Boot 4 dùng Jackson 3 còn một phần thư viện dùng Jackson 2, vốn có thể
 * khiến phần thân yêu cầu không được ghi ra (FastAPI trả 422 "Field required: body").
 */
@Service
@Transactional(readOnly = true)
public class AiPredictionService {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final BigDecimal DEFAULT_LOAD_PERCENT = new BigDecimal("50");
    private static final BigDecimal DEFAULT_HISTORICAL_SPEED = new BigDecimal("50");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final TripRepository tripRepository;
    private final GpsLogRepository gpsLogRepository;
    private final TripOrderRepository tripOrderRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String analyzeUrl;

    public AiPredictionService(TripRepository tripRepository, GpsLogRepository gpsLogRepository,
            TripOrderRepository tripOrderRepository, ObjectMapper objectMapper,
            @Value("${ai-service.base-url}") String aiServiceBaseUrl) {
        this.tripRepository = tripRepository;
        this.gpsLogRepository = gpsLogRepository;
        this.tripOrderRepository = tripOrderRepository;
        this.objectMapper = objectMapper;
        // Bắt buộc HTTP/1.1: HttpClient của JDK mặc định thử nâng cấp h2c và uvicorn
        // từ chối "upgrade request", khiến phần thân yêu cầu bị mất (FastAPI trả 422).
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.analyzeUrl = aiServiceBaseUrl.replaceAll("/+$", "") + "/v1/predictions/analyze";
    }

    public TripAnalysisResponse analyze(UUID tripId, TripAnalysisRequest overrides) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chuyến xe"));
        GpsLog latest = gpsLogRepository.findFirstByTripIdOrderByRecordedAtDesc(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Chuyến xe chưa có dữ liệu GPS để AI phân tích"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("speed_kmh", overrides.speedKmh() != null ? overrides.speedKmh() : latest.getSpeedKmh());
        payload.put("route_deviation_meters",
                overrides.routeDeviationMeters() != null ? overrides.routeDeviationMeters() : BigDecimal.ZERO);
        payload.put("stopped_minutes", overrides.stoppedMinutes() != null ? overrides.stoppedMinutes() : 0);
        payload.put("remaining_distance_km",
                overrides.remainingDistanceKm() != null ? overrides.remainingDistanceKm() : BigDecimal.ZERO);
        payload.put("hour_of_day", LocalDateTime.now().getHour());
        payload.put("load_percent", tripLoadPercent(trip));
        payload.put("historical_avg_speed_kmh", historicalAverageSpeed(tripId));

        Map<String, Object> response = postAnalyze(payload);
        if (response == null) {
            throw unavailable("AI service trả về phản hồi rỗng");
        }
        return new TripAnalysisResponse(tripId, decimal(response.get("eta_minutes")),
                decimal(response.get("late_probability")), text(response.get("risk_level")),
                decimal(response.get("anomaly_score")), text(response.get("model_version")),
                bool(response.get("using_baseline")), textList(response.get("explanations")));
    }

    private Map<String, Object> postAnalyze(Map<String, Object> payload) {
        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw unavailable("Không thể tạo yêu cầu JSON cho AI service");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(analyzeUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable("Yêu cầu tới AI service bị ngắt");
        } catch (Exception exception) {
            throw unavailable("Không kết nối được AI service (" + exception.getClass().getSimpleName() + ")");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw unavailable("AI service phản hồi lỗi HTTP " + response.statusCode() + detailOf(response.body()));
        }
        try {
            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (Exception exception) {
            throw unavailable("Không đọc được JSON phản hồi của AI service");
        }
    }

    /** Tỷ lệ tải thực tế của chuyến trên tải trọng xe; 50% nếu chưa đủ dữ liệu. */
    private BigDecimal tripLoadPercent(Trip trip) {
        BigDecimal maxPayload = trip.getVehicle() == null ? null : trip.getVehicle().getMaxPayloadKg();
        if (maxPayload == null || maxPayload.signum() <= 0) {
            return DEFAULT_LOAD_PERCENT;
        }
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (TripOrder tripOrder : tripOrderRepository.findByTripIdOrderBySequenceNo(trip.getId())) {
            if (tripOrder.getTransportOrder() != null && tripOrder.getTransportOrder().getWeightKg() != null) {
                totalWeight = totalWeight.add(tripOrder.getTransportOrder().getWeightKg());
            }
        }
        if (totalWeight.signum() <= 0) {
            return DEFAULT_LOAD_PERCENT;
        }
        return totalWeight.multiply(new BigDecimal("100")).divide(maxPayload, 2, RoundingMode.HALF_UP)
                .min(new BigDecimal("100"));
    }

    /** Tốc độ trung bình đã ghi nhận của chuyến; 50 km/h (mặc định thận trọng) nếu chưa có GPS. */
    private BigDecimal historicalAverageSpeed(UUID tripId) {
        BigDecimal average = gpsLogRepository.averageSpeedKmhByTripId(tripId);
        return average == null || average.signum() <= 0 ? DEFAULT_HISTORICAL_SPEED : average.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal decimal(Object value) {
        return value == null ? null : new BigDecimal(value.toString());
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static boolean bool(Object value) {
        return value instanceof Boolean flag ? flag : Boolean.parseBoolean(String.valueOf(value));
    }

    private static List<String> textList(Object value) {
        if (!(value instanceof List<?> items)) {
            return List.of();
        }
        List<String> result = new ArrayList<>(items.size());
        for (Object item : items) {
            result.add(String.valueOf(item));
        }
        return result;
    }

    /** Trích ngắn nội dung lỗi FastAPI để chẩn đoán hợp đồng dữ liệu khi tích hợp. */
    private static String detailOf(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        String compact = responseBody.replaceAll("\\s+", " ").trim();
        return ": " + (compact.length() > 300 ? compact.substring(0, 300) + "…" : compact);
    }

    private ResponseStatusException unavailable(String detail) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                detail + ". Hãy kiểm tra FastAPI và cấu hình AI_SERVICE_BASE_URL.");
    }
}
