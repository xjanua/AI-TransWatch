package me.xjanua.aitranswatch.dto.report;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Số liệu tổng hợp cho tab báo cáo của dashboard. */
public record ReportOverviewResponse(
        Map<String, Long> tripsByStatus,
        Map<String, Long> ordersByStatus,
        Map<String, Long> vehiclesByStatus,
        Map<String, Long> alertsBySeverity,
        Map<String, Long> alertsByType,
        List<DailyCount> tripsPerDay,
        List<DailyCount> alertsPerDay,
        double onTimeRate,
        long completedTripsWithEta,
        double alertHandlingRate,
        long totalAlerts,
        long totalTrips,
        long totalOrders) {

    public record DailyCount(LocalDate date, long count) {
    }
}
