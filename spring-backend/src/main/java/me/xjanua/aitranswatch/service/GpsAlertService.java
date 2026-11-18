package me.xjanua.aitranswatch.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import me.xjanua.aitranswatch.dto.alert.AlertCreatedEvent;
import me.xjanua.aitranswatch.dto.alert.AlertResponse;
import me.xjanua.aitranswatch.dto.alert.AlertStatusUpdateRequest;
import me.xjanua.aitranswatch.dto.gps.GpsIngestionRequest;
import me.xjanua.aitranswatch.dto.gps.GpsLogResponse;
import me.xjanua.aitranswatch.enums.AlertSeverity;
import me.xjanua.aitranswatch.enums.AlertStatus;
import me.xjanua.aitranswatch.enums.AlertType;
import me.xjanua.aitranswatch.model.Alert;
import me.xjanua.aitranswatch.model.GpsLog;
import me.xjanua.aitranswatch.model.Trip;
import me.xjanua.aitranswatch.model.TripRoutePoint;
import me.xjanua.aitranswatch.model.User;
import me.xjanua.aitranswatch.repository.AlertRepository;
import me.xjanua.aitranswatch.repository.GpsLogRepository;
import me.xjanua.aitranswatch.repository.TripRepository;
import me.xjanua.aitranswatch.repository.TripRoutePointRepository;
import me.xjanua.aitranswatch.repository.UserRepository;
import me.xjanua.aitranswatch.util.GeoUtils;

/**
 * Nhận telemetry GPS và chạy rule engine cảnh báo vận hành.
 *
 * <p>Các ngưỡng đều cấu hình được qua biến môi trường/thuộc tính (giá trị mặc định dùng cho demo),
 * và mọi cảnh báo đều lưu {@code evidence} dạng JSON để giải thích lý do cho điều phối viên.
 */
@Service
@Transactional
public class GpsAlertService {
    private static final EnumSet<AlertStatus> OPEN_STATUSES = EnumSet.of(AlertStatus.NEW, AlertStatus.IN_PROGRESS);

    private final TripRepository tripRepository;
    private final GpsLogRepository gpsLogRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final TripRoutePointRepository routePointRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final BigDecimal speedLimitKmh;
    private final long routeDeviationMeters;
    private final int abnormalStopMinutes;
    private final int continuousDrivingMinutes;
    private final int duplicateSuppressionHours;

    public GpsAlertService(TripRepository tripRepository, GpsLogRepository gpsLogRepository,
            AlertRepository alertRepository, UserRepository userRepository, TripRoutePointRepository routePointRepository,
            ApplicationEventPublisher eventPublisher,
            @Value("${alerts.speed-limit-kmh:80}") BigDecimal speedLimitKmh,
            @Value("${alerts.route-deviation-meters:300}") long routeDeviationMeters,
            @Value("${alerts.abnormal-stop-minutes:15}") int abnormalStopMinutes,
            @Value("${alerts.continuous-driving-minutes:240}") int continuousDrivingMinutes,
            @Value("${alerts.duplicate-suppression-hours:6}") int duplicateSuppressionHours) {
        this.tripRepository = tripRepository;
        this.gpsLogRepository = gpsLogRepository;
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.routePointRepository = routePointRepository;
        this.eventPublisher = eventPublisher;
        this.speedLimitKmh = speedLimitKmh;
        this.routeDeviationMeters = routeDeviationMeters;
        this.abnormalStopMinutes = abnormalStopMinutes;
        this.continuousDrivingMinutes = continuousDrivingMinutes;
        this.duplicateSuppressionHours = duplicateSuppressionHours;
    }

    public GpsLogResponse ingest(UUID tripId, GpsIngestionRequest request, Authentication authentication) {
        Trip trip = trip(tripId);
        verifyCanIngest(trip, authentication);
        LocalDateTime recordedAt = request.recordedAt() == null ? LocalDateTime.now() : request.recordedAt();
        GpsLog log = gpsLogRepository.save(new GpsLog(trip, request.latitude(), request.longitude(), request.speedKmh(),
                request.headingDegree(), Boolean.TRUE.equals(request.engineOn()), request.stoppedMinutes(), recordedAt));
        evaluateRules(trip, request, recordedAt);
        return toGpsResponse(log);
    }

    @Transactional(readOnly = true)
    public GpsLogResponse latest(UUID tripId) {
        trip(tripId);
        return gpsLogRepository.findFirstByTripIdOrderByRecordedAtDesc(tripId)
                .map(this::toGpsResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chuyến xe chưa có dữ liệu GPS"));
    }

    @Transactional(readOnly = true)
    public Page<AlertResponse> list(AlertStatus status, AlertSeverity severity, UUID tripId, Pageable pageable) {
        Page<Alert> alerts;
        if (status != null && severity != null && tripId != null) alerts = alertRepository.findByStatusAndSeverityAndTripId(status, severity, tripId, pageable);
        else if (status != null && severity != null) alerts = alertRepository.findByStatusAndSeverity(status, severity, pageable);
        else if (status != null && tripId != null) alerts = alertRepository.findByStatusAndTripId(status, tripId, pageable);
        else if (severity != null && tripId != null) alerts = alertRepository.findBySeverityAndTripId(severity, tripId, pageable);
        else if (status != null) alerts = alertRepository.findByStatus(status, pageable);
        else if (severity != null) alerts = alertRepository.findBySeverity(severity, pageable);
        else if (tripId != null) alerts = alertRepository.findByTripId(tripId, pageable);
        else alerts = alertRepository.findAll(pageable);
        return alerts.map(this::toAlertResponse);
    }

    public AlertResponse updateStatus(UUID id, AlertStatusUpdateRequest request, Authentication authentication) {
        Alert alert = alertRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cảnh báo"));
        User user = userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không hợp lệ"));
        alert.updateStatus(request.status(), user, request.handlingNote());
        return toAlertResponse(alert);
    }

    /** Ghi telemetry từ trình mô phỏng demo (bỏ qua kiểm tra quyền vì controller đã giới hạn admin). */
    public GpsLogResponse ingestSimulation(UUID tripId, GpsIngestionRequest request) {
        Trip trip = trip(tripId);
        LocalDateTime recordedAt = request.recordedAt() == null ? LocalDateTime.now() : request.recordedAt();
        GpsLog log = gpsLogRepository.save(new GpsLog(trip, request.latitude(), request.longitude(), request.speedKmh(),
                request.headingDegree(), Boolean.TRUE.equals(request.engineOn()), request.stoppedMinutes(), recordedAt));
        evaluateRules(trip, request, recordedAt);
        return toGpsResponse(log);
    }

    /** Chạy toàn bộ luật cảnh báo cho một bản ghi telemetry vừa nhận. */
    private void evaluateRules(Trip trip, GpsIngestionRequest request, LocalDateTime occurredAt) {
        if (request.speedKmh().compareTo(speedLimitKmh) > 0) {
            createTripAlert(trip, AlertType.SPEEDING, AlertSeverity.HIGH, occurredAt,
                    "{\"speedKmh\":" + request.speedKmh() + ",\"speedLimitKmh\":" + speedLimitKmh + "}");
        }
        if (Boolean.TRUE.equals(request.engineOn()) && request.speedKmh().compareTo(BigDecimal.ONE) <= 0
                && request.stoppedMinutes() != null && request.stoppedMinutes() >= abnormalStopMinutes) {
            createTripAlert(trip, AlertType.ABNORMAL_STOP, AlertSeverity.MEDIUM, occurredAt,
                    "{\"speedKmh\":" + request.speedKmh() + ",\"stoppedMinutes\":" + request.stoppedMinutes()
                            + ",\"thresholdMinutes\":" + abnormalStopMinutes + "}");
        }
        evaluateRouteDeviation(trip, request, occurredAt);
        evaluateContinuousDriving(trip, occurredAt);
    }

    /** Lệch tuyến: khoảng cách từ vị trí hiện tại tới lộ trình dự kiến vượt ngưỡng. */
    private void evaluateRouteDeviation(Trip trip, GpsIngestionRequest request, LocalDateTime occurredAt) {
        List<TripRoutePoint> route = routePointRepository.findByTripIdOrderBySequenceNo(trip.getId());
        Double distance = GeoUtils.distanceToRouteMeters(route, request.latitude().doubleValue(), request.longitude().doubleValue());
        if (distance == null || distance <= routeDeviationMeters) {
            return;
        }
        AlertSeverity severity = distance > routeDeviationMeters * 2 ? AlertSeverity.HIGH : AlertSeverity.MEDIUM;
        createTripAlert(trip, AlertType.ROUTE_DEVIATION, severity, occurredAt,
                "{\"deviationMeters\":" + Math.round(distance) + ",\"allowedMeters\":" + routeDeviationMeters + "}");
    }

    /**
     * Lái liên tục: thời gian lái kể từ lần dừng nghỉ đủ dài gần nhất vượt ngưỡng cho phép.
     *
     * <p>Chỉ tính các bản ghi không muộn hơn thời điểm telemetry hiện tại, nên telemetry lùi giờ
     * (dùng cho trình mô phỏng) vẫn cho kết quả đúng thay vì tạo khoảng thời gian âm.
     */
    private void evaluateContinuousDriving(Trip trip, LocalDateTime occurredAt) {
        LocalDateTime startedAt = trip.getActualStartAt() == null ? trip.getScheduledStartAt() : trip.getActualStartAt();
        LocalDateTime fallback = startedAt.isAfter(occurredAt) ? occurredAt : startedAt;
        LocalDateTime reference = gpsLogRepository
                .findFirstByTripIdAndStoppedMinutesGreaterThanEqualAndRecordedAtLessThanEqualOrderByRecordedAtDesc(
                        trip.getId(), abnormalStopMinutes, occurredAt)
                .map(GpsLog::getRecordedAt)
                .orElseGet(() -> gpsLogRepository
                        .findFirstByTripIdAndRecordedAtLessThanEqualOrderByRecordedAtAsc(trip.getId(), occurredAt)
                        .map(GpsLog::getRecordedAt)
                        .orElse(fallback));
        long drivingMinutes = java.time.Duration.between(reference, occurredAt).toMinutes();
        if (drivingMinutes < continuousDrivingMinutes) {
            return;
        }
        createTripAlert(trip, AlertType.CONTINUOUS_DRIVING, AlertSeverity.MEDIUM, occurredAt,
                "{\"drivingMinutes\":" + drivingMinutes + ",\"thresholdMinutes\":" + continuousDrivingMinutes
                        + ",\"requiredRestMinutes\":" + abnormalStopMinutes + "}");
    }

    /** Tạo cảnh báo cho chuyến, bỏ qua nếu đã có cảnh báo cùng loại còn mở trong thời gian gần đây. */
    private void createTripAlert(Trip trip, AlertType type, AlertSeverity severity, LocalDateTime occurredAt, String evidence) {
        boolean duplicated = alertRepository.existsByAlertTypeAndTripIdAndStatusInAndOccurredAtAfter(
                type, trip.getId(), OPEN_STATUSES, occurredAt.minusHours(duplicateSuppressionHours));
        if (duplicated) {
            return;
        }
        Alert alert = alertRepository.save(new Alert(trip, type, severity, evidence, occurredAt));
        eventPublisher.publishEvent(new AlertCreatedEvent(toAlertResponse(alert)));
    }

    private void verifyCanIngest(Trip trip, Authentication authentication) {
        boolean operator = authentication.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DISPATCHER"));
        if (operator) return;
        if (trip.getDriver().getUser() == null || !trip.getDriver().getUser().getUsername().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài xế chỉ được gửi GPS cho chuyến được giao");
        }
    }

    private Trip trip(UUID id) {
        return tripRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chuyến xe"));
    }

    private GpsLogResponse toGpsResponse(GpsLog log) {
        return new GpsLogResponse(log.getId(), log.getTrip().getId(), log.getVehicle().getId(), log.getLatitude(), log.getLongitude(),
                log.getSpeedKmh(), log.getHeadingDegree(), log.isEngineOn(), log.getRecordedAt());
    }

    private AlertResponse toAlertResponse(Alert alert) {
        return new AlertResponse(alert.getId(), alert.getTrip() == null ? null : alert.getTrip().getId(),
                alert.getVehicle() == null ? null : alert.getVehicle().getId(), alert.getDriver() == null ? null : alert.getDriver().getId(),
                alert.getAlertType(), alert.getSeverity(), alert.getStatus(), alert.getEvidence(), alert.getOccurredAt(),
                alert.getHandledBy() == null ? null : alert.getHandledBy().getId(), alert.getHandledAt(), alert.getHandlingNote());
    }
}
