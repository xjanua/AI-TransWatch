package me.xjanua.aitranswatch.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.xjanua.aitranswatch.dto.alert.AlertCreatedEvent;
import me.xjanua.aitranswatch.dto.alert.AlertResponse;
import me.xjanua.aitranswatch.enums.AlertStatus;
import me.xjanua.aitranswatch.enums.AlertType;
import me.xjanua.aitranswatch.enums.AlertSeverity;
import me.xjanua.aitranswatch.enums.DriverStatus;
import me.xjanua.aitranswatch.model.Alert;
import me.xjanua.aitranswatch.model.Driver;
import me.xjanua.aitranswatch.model.GpsLog;
import me.xjanua.aitranswatch.model.Trip;
import me.xjanua.aitranswatch.model.Vehicle;
import me.xjanua.aitranswatch.enums.TripStatus;
import me.xjanua.aitranswatch.repository.AlertRepository;
import me.xjanua.aitranswatch.repository.DriverRepository;
import me.xjanua.aitranswatch.repository.GpsLogRepository;
import me.xjanua.aitranswatch.repository.TripRepository;
import me.xjanua.aitranswatch.repository.VehicleRepository;

/**
 * Kiểm tra định kỳ các điều kiện cảnh báo không gắn với một điểm telemetry cụ thể:
 * mất tín hiệu GPS, giấy tờ sắp hết hạn và xe đến hạn bảo dưỡng.
 *
 * <p>Mọi cảnh báo đều chống trùng theo loại + đối tượng trong khoảng thời gian cấu hình,
 * nên chạy lặp lại không tạo ra hàng loạt bản ghi trùng.
 */
@Service
public class FleetAlertScheduler {
    private static final Logger log = LoggerFactory.getLogger(FleetAlertScheduler.class);
    private static final EnumSet<AlertStatus> OPEN_STATUSES = EnumSet.of(AlertStatus.NEW, AlertStatus.IN_PROGRESS);
    private static final List<TripStatus> MONITORED_TRIP_STATUSES =
            List.of(TripStatus.SCHEDULED, TripStatus.PICKUP_IN_PROGRESS, TripStatus.IN_TRANSIT, TripStatus.INCIDENT);

    private final TripRepository tripRepository;
    private final GpsLogRepository gpsLogRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final AlertRepository alertRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final long gpsSignalTimeoutMinutes;
    private final long documentExpiryWarningDays;
    private final int duplicateSuppressionHours;

    public FleetAlertScheduler(TripRepository tripRepository, GpsLogRepository gpsLogRepository,
            VehicleRepository vehicleRepository, DriverRepository driverRepository, AlertRepository alertRepository,
            ApplicationEventPublisher eventPublisher,
            @Value("${alerts.gps-signal-timeout-minutes:10}") long gpsSignalTimeoutMinutes,
            @Value("${alerts.document-expiry-warning-days:30}") long documentExpiryWarningDays,
            @Value("${alerts.duplicate-suppression-hours:6}") int duplicateSuppressionHours) {
        this.tripRepository = tripRepository;
        this.gpsLogRepository = gpsLogRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.alertRepository = alertRepository;
        this.eventPublisher = eventPublisher;
        this.gpsSignalTimeoutMinutes = gpsSignalTimeoutMinutes;
        this.documentExpiryWarningDays = documentExpiryWarningDays;
        this.duplicateSuppressionHours = duplicateSuppressionHours;
    }

    @Transactional
    public int runChecks() {
        int created = checkGpsSignal() + checkVehicleDocuments() + checkDriverLicenses() + checkMaintenance();
        if (created > 0) {
            log.info("Đã tạo {} cảnh báo định kỳ (GPS/giấy tờ/bảo dưỡng)", created);
        }
        return created;
    }

    /** Chuyến đang chạy nhưng không nhận được telemetry trong khoảng thời gian cho phép. */
    int checkGpsSignal() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(gpsSignalTimeoutMinutes);
        int created = 0;
        for (Trip trip : tripRepository.findByStatusInOrderByScheduledStartAtAsc(MONITORED_TRIP_STATUSES)) {
            GpsLog latest = gpsLogRepository.findFirstByTripIdOrderByRecordedAtDesc(trip.getId()).orElse(null);
            if (latest == null || latest.getRecordedAt().isAfter(threshold)) {
                continue;
            }
            long silentMinutes = java.time.Duration.between(latest.getRecordedAt(), LocalDateTime.now()).toMinutes();
            created += createTripAlert(trip, AlertType.GPS_SIGNAL_LOST, AlertSeverity.MEDIUM,
                    "{\"silentMinutes\":" + silentMinutes + ",\"timeoutMinutes\":" + gpsSignalTimeoutMinutes + "}",
                    latest.getRecordedAt());
        }
        return created;
    }

    /** Đăng kiểm hoặc bảo hiểm xe sắp hết hạn trong số ngày cấu hình. */
    int checkVehicleDocuments() {
        LocalDate deadline = LocalDate.now().plusDays(documentExpiryWarningDays);
        int created = 0;
        for (Vehicle vehicle : vehicleRepository.findAll()) {
            created += expiryAlert(vehicle, vehicle.getRegistrationExpiryDate(), "registration", deadline);
            created += expiryAlert(vehicle, vehicle.getInsuranceExpiryDate(), "insurance", deadline);
        }
        return created;
    }

    private int expiryAlert(Vehicle vehicle, LocalDate expiryDate, String document, LocalDate deadline) {
        if (expiryDate == null || expiryDate.isAfter(deadline)) {
            return 0;
        }
        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
        AlertSeverity severity = daysLeft <= 0 ? AlertSeverity.HIGH : daysLeft <= 7 ? AlertSeverity.MEDIUM : AlertSeverity.LOW;
        String evidence = "{\"document\":\"" + document + "\",\"expiryDate\":\"" + expiryDate + "\",\"daysLeft\":" + daysLeft + "}";
        boolean duplicated = alertRepository.existsByAlertTypeAndVehicleIdAndStatusInAndOccurredAtAfter(
                AlertType.DOCUMENT_EXPIRING, vehicle.getId(), OPEN_STATUSES, LocalDateTime.now().minusHours(duplicateSuppressionHours));
        if (duplicated) {
            return 0;
        }
        publish(alertRepository.save(new Alert(vehicle, null, AlertType.DOCUMENT_EXPIRING, severity, evidence, LocalDateTime.now())));
        return 1;
    }

    /** Giấy phép lái xe của tài xế còn hiệu lực nhưng sắp hết hạn. */
    int checkDriverLicenses() {
        LocalDate deadline = LocalDate.now().plusDays(documentExpiryWarningDays);
        int created = 0;
        for (Driver driver : driverRepository.findAll()) {
            if (driver.getLicenseExpiryDate() == null || driver.getLicenseExpiryDate().isAfter(deadline)
                    || driver.getStatus() == DriverStatus.SUSPENDED) {
                continue;
            }
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), driver.getLicenseExpiryDate());
            AlertSeverity severity = daysLeft <= 0 ? AlertSeverity.HIGH : daysLeft <= 7 ? AlertSeverity.MEDIUM : AlertSeverity.LOW;
            boolean duplicated = alertRepository.existsByAlertTypeAndDriverIdAndStatusInAndOccurredAtAfter(
                    AlertType.DOCUMENT_EXPIRING, driver.getId(), OPEN_STATUSES, LocalDateTime.now().minusHours(duplicateSuppressionHours));
            if (duplicated) {
                continue;
            }
            String evidence = "{\"document\":\"driver-license\",\"expiryDate\":\"" + driver.getLicenseExpiryDate() + "\",\"daysLeft\":" + daysLeft + "}";
            publish(alertRepository.save(new Alert(null, driver, AlertType.DOCUMENT_EXPIRING, severity, evidence, LocalDateTime.now())));
            created++;
        }
        return created;
    }

    /** Xe đã chạy tới hoặc vượt mốc bảo dưỡng kế tiếp. */
    int checkMaintenance() {
        int created = 0;
        for (Vehicle vehicle : vehicleRepository.findAll()) {
            if (vehicle.getNextMaintenanceOdometerKm() == null || vehicle.getCurrentOdometerKm() == null
                    || vehicle.getCurrentOdometerKm().compareTo(vehicle.getNextMaintenanceOdometerKm()) < 0) {
                continue;
            }
            boolean duplicated = alertRepository.existsByAlertTypeAndVehicleIdAndStatusInAndOccurredAtAfter(
                    AlertType.MAINTENANCE_DUE, vehicle.getId(), OPEN_STATUSES, LocalDateTime.now().minusHours(duplicateSuppressionHours));
            if (duplicated) {
                continue;
            }
            String evidence = "{\"currentOdometerKm\":" + vehicle.getCurrentOdometerKm()
                    + ",\"nextMaintenanceOdometerKm\":" + vehicle.getNextMaintenanceOdometerKm() + "}";
            publish(alertRepository.save(new Alert(vehicle, null, AlertType.MAINTENANCE_DUE, AlertSeverity.MEDIUM, evidence, LocalDateTime.now())));
            created++;
        }
        return created;
    }

    private int createTripAlert(Trip trip, AlertType type, AlertSeverity severity, String evidence, LocalDateTime occurredAt) {
        boolean duplicated = alertRepository.existsByAlertTypeAndTripIdAndStatusInAndOccurredAtAfter(
                type, trip.getId(), OPEN_STATUSES, LocalDateTime.now().minusHours(duplicateSuppressionHours));
        if (duplicated) {
            return 0;
        }
        publish(alertRepository.save(new Alert(trip, type, severity, evidence, occurredAt)));
        return 1;
    }

    /** Phát sự kiện để cảnh báo được đẩy realtime tới dashboard sau khi giao dịch commit. */
    private void publish(Alert alert) {
        eventPublisher.publishEvent(new AlertCreatedEvent(new AlertResponse(alert.getId(),
                alert.getTrip() == null ? null : alert.getTrip().getId(),
                alert.getVehicle() == null ? null : alert.getVehicle().getId(),
                alert.getDriver() == null ? null : alert.getDriver().getId(),
                alert.getAlertType(), alert.getSeverity(), alert.getStatus(), alert.getEvidence(), alert.getOccurredAt(),
                null, null, null)));
    }
}
