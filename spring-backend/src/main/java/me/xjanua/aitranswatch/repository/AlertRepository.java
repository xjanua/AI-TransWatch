package me.xjanua.aitranswatch.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import me.xjanua.aitranswatch.enums.AlertSeverity;
import me.xjanua.aitranswatch.enums.AlertStatus;
import me.xjanua.aitranswatch.enums.AlertType;
import me.xjanua.aitranswatch.model.Alert;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    Page<Alert> findByStatus(AlertStatus status, Pageable pageable);
    Page<Alert> findBySeverity(AlertSeverity severity, Pageable pageable);
    Page<Alert> findByTripId(UUID tripId, Pageable pageable);
    Page<Alert> findByStatusAndSeverity(AlertStatus status, AlertSeverity severity, Pageable pageable);
    Page<Alert> findByStatusAndTripId(AlertStatus status, UUID tripId, Pageable pageable);
    Page<Alert> findBySeverityAndTripId(AlertSeverity severity, UUID tripId, Pageable pageable);
    Page<Alert> findByStatusAndSeverityAndTripId(AlertStatus status, AlertSeverity severity, UUID tripId, Pageable pageable);

    /** Số cảnh báo theo mức độ cho báo cáo. */
    @Query("select a.severity, count(a) from Alert a group by a.severity")
    List<Object[]> countBySeverity();

    /** Số cảnh báo theo loại cho báo cáo. */
    @Query("select a.alertType, count(a) from Alert a group by a.alertType order by count(a) desc")
    List<Object[]> countByType();

    /** Số cảnh báo theo ngày trong khoảng thời gian, phục vụ biểu đồ. */
    @Query("""
            select year(a.occurredAt), month(a.occurredAt), day(a.occurredAt), count(a)
            from Alert a where a.occurredAt >= :from
            group by year(a.occurredAt), month(a.occurredAt), day(a.occurredAt)
            order by year(a.occurredAt), month(a.occurredAt), day(a.occurredAt)""")
    List<Object[]> countByDaySince(@Param("from") LocalDateTime from);

    /** Số cảnh báo đã xử lý xong trên tổng số cảnh báo phát sinh. */
    @Query("select count(a) from Alert a where a.status in (me.xjanua.aitranswatch.enums.AlertStatus.RESOLVED, me.xjanua.aitranswatch.enums.AlertStatus.DISMISSED)")
    long countHandled();

    /** Chặn tạo trùng cảnh báo cùng loại cho cùng chuyến trong khoảng thời gian gần đây. */
    boolean existsByAlertTypeAndTripIdAndStatusInAndOccurredAtAfter(
            AlertType alertType, UUID tripId, Collection<AlertStatus> statuses, LocalDateTime after);

    /** Chặn tạo trùng cảnh báo cùng loại cho cùng xe (cảnh báo không gắn chuyến). */
    boolean existsByAlertTypeAndVehicleIdAndStatusInAndOccurredAtAfter(
            AlertType alertType, UUID vehicleId, Collection<AlertStatus> statuses, LocalDateTime after);

    /** Chặn tạo trùng cảnh báo cùng loại cho cùng tài xế (ví dụ giấy phép lái xe sắp hết hạn). */
    boolean existsByAlertTypeAndDriverIdAndStatusInAndOccurredAtAfter(
            AlertType alertType, UUID driverId, Collection<AlertStatus> statuses, LocalDateTime after);

    /** Cảnh báo đang mở của một chuyến, dùng khi cần đối chiếu trạng thái xử lý. */
    List<Alert> findByTripIdAndStatusIn(UUID tripId, Collection<AlertStatus> statuses);
}
