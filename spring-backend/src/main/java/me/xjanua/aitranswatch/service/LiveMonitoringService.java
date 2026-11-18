package me.xjanua.aitranswatch.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.xjanua.aitranswatch.dto.monitoring.LiveVehicleResponse;
import me.xjanua.aitranswatch.enums.TripStatus;
import me.xjanua.aitranswatch.model.Driver;
import me.xjanua.aitranswatch.model.GpsLog;
import me.xjanua.aitranswatch.model.Trip;
import me.xjanua.aitranswatch.repository.GpsLogRepository;
import me.xjanua.aitranswatch.repository.TripRepository;

/**
 * Giám sát đội xe đang vận hành: trả vị trí GPS mới nhất của mọi chuyến chưa kết thúc.
 *
 * <p>Dùng cho bản đồ trực tuyến ở dashboard. Với dữ liệu GPS thật quy mô lớn, truy vấn này
 * nên được thay bằng view/bảng "vị trí mới nhất" hoặc cache realtime.
 */
@Service
@Transactional(readOnly = true)
public class LiveMonitoringService {
    private static final List<TripStatus> RUNNING_STATUSES = List.of(
            TripStatus.SCHEDULED,
            TripStatus.PICKUP_IN_PROGRESS,
            TripStatus.IN_TRANSIT,
            TripStatus.INCIDENT);

    private final TripRepository tripRepository;
    private final GpsLogRepository gpsLogRepository;

    public LiveMonitoringService(TripRepository tripRepository, GpsLogRepository gpsLogRepository) {
        this.tripRepository = tripRepository;
        this.gpsLogRepository = gpsLogRepository;
    }

    public List<LiveVehicleResponse> liveFleet() {
        List<Trip> trips = tripRepository.findByStatusInOrderByScheduledStartAtAsc(RUNNING_STATUSES);
        List<LiveVehicleResponse> result = new ArrayList<>(trips.size());
        for (Trip trip : trips) {
            GpsLog latest = gpsLogRepository.findFirstByTripIdOrderByRecordedAtDesc(trip.getId()).orElse(null);
            result.add(new LiveVehicleResponse(
                    trip.getId(),
                    trip.getTripCode(),
                    trip.getStatus(),
                    trip.getVehicle() == null ? null : trip.getVehicle().getId(),
                    trip.getVehicle() == null ? null : trip.getVehicle().getLicensePlate(),
                    driverName(trip.getDriver()),
                    latest == null ? null : latest.getLatitude(),
                    latest == null ? null : latest.getLongitude(),
                    latest == null ? null : latest.getSpeedKmh(),
                    latest == null ? null : latest.getHeadingDegree(),
                    latest == null ? null : latest.isEngineOn(),
                    latest == null ? null : latest.getRecordedAt()));
        }
        return result;
    }

    private static String driverName(Driver driver) {
        if (driver == null) {
            return null;
        }
        if (driver.getUser() != null && driver.getUser().getFullName() != null) {
            return driver.getUser().getFullName();
        }
        return driver.getEmployeeCode();
    }
}
