package me.xjanua.aitranswatch.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.xjanua.aitranswatch.dto.report.ReportOverviewResponse;
import me.xjanua.aitranswatch.dto.report.ReportOverviewResponse.DailyCount;
import me.xjanua.aitranswatch.repository.AlertRepository;
import me.xjanua.aitranswatch.repository.TransportOrderRepository;
import me.xjanua.aitranswatch.repository.TripRepository;
import me.xjanua.aitranswatch.repository.VehicleRepository;

/**
 * Tổng hợp số liệu báo cáo cho dashboard.
 *
 * <p>Các phép đếm được thực hiện bằng truy vấn gộp (group by) ở tầng repository thay vì tải
 * toàn bộ bản ghi về ứng dụng, để vẫn hiệu quả khi dữ liệu vận hành tăng lên.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {
    private static final int CHART_DAYS = 7;

    private final TripRepository tripRepository;
    private final AlertRepository alertRepository;
    private final TransportOrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;

    public ReportService(TripRepository tripRepository, AlertRepository alertRepository,
            TransportOrderRepository orderRepository, VehicleRepository vehicleRepository) {
        this.tripRepository = tripRepository;
        this.alertRepository = alertRepository;
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public ReportOverviewResponse overview() {
        LocalDateTime from = LocalDate.now().minusDays(CHART_DAYS - 1L).atStartOfDay();

        Map<String, Long> tripsByStatus = toMap(tripRepository.countByStatus());
        Map<String, Long> ordersByStatus = toMap(orderRepository.countByStatus());
        Map<String, Long> vehiclesByStatus = toMap(vehicleRepository.countByStatus());
        Map<String, Long> alertsBySeverity = toMap(alertRepository.countBySeverity());
        Map<String, Long> alertsByType = toMap(alertRepository.countByType());

        long totalAlerts = alertsBySeverity.values().stream().mapToLong(Long::longValue).sum();
        long handledAlerts = alertRepository.countHandled();
        long completedWithEta = tripRepository.countCompletedWithEta();
        long onTime = tripRepository.countOnTimeCompleted();

        return new ReportOverviewResponse(
                tripsByStatus,
                ordersByStatus,
                vehiclesByStatus,
                alertsBySeverity,
                alertsByType,
                dailySeries(tripRepository.countByDaySince(from)),
                dailySeries(alertRepository.countByDaySince(from)),
                completedWithEta == 0 ? 0d : (double) onTime / completedWithEta,
                completedWithEta,
                totalAlerts == 0 ? 0d : (double) handledAlerts / totalAlerts,
                totalAlerts,
                sum(tripsByStatus),
                sum(ordersByStatus));
    }

    private static long sum(Map<String, Long> values) {
        return values.values().stream().mapToLong(Long::longValue).sum();
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return result;
    }

    /** Chuỗi theo ngày, bù các ngày không có dữ liệu để biểu đồ không bị đứt đoạn. */
    private static List<DailyCount> dailySeries(List<Object[]> rows) {
        Map<LocalDate, Long> byDate = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate date = LocalDate.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue(), ((Number) row[2]).intValue());
            byDate.merge(date, ((Number) row[3]).longValue(), Long::sum);
        }
        List<DailyCount> series = new ArrayList<>(CHART_DAYS);
        LocalDate start = LocalDate.now().minusDays(CHART_DAYS - 1L);
        for (int offset = 0; offset < CHART_DAYS; offset++) {
            LocalDate date = start.plusDays(offset);
            series.add(new DailyCount(date, byDate.getOrDefault(date, 0L)));
        }
        return series;
    }
}
