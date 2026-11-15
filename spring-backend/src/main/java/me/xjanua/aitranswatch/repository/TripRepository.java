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

import me.xjanua.aitranswatch.enums.TripStatus;
import me.xjanua.aitranswatch.model.Trip;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    boolean existsByTripCodeIgnoreCase(String tripCode);
    Page<Trip> findByStatus(TripStatus status, Pageable pageable);
    Page<Trip> findByVehicleId(UUID vehicleId, Pageable pageable);
    Page<Trip> findByDriverId(UUID driverId, Pageable pageable);
    Page<Trip> findByStatusAndVehicleId(TripStatus status, UUID vehicleId, Pageable pageable);
    Page<Trip> findByStatusAndDriverId(TripStatus status, UUID driverId, Pageable pageable);
    List<Trip> findByStatusInOrderByScheduledStartAtAsc(Collection<TripStatus> statuses);
    @Query("select count(t) > 0 from Trip t where t.vehicle.id = :vehicleId and t.status not in :terminal and t.scheduledStartAt < :end and coalesce(t.estimatedArrivalAt, t.scheduledStartAt) > :start")
    boolean hasVehicleConflict(@Param("vehicleId") UUID vehicleId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("terminal") Collection<TripStatus> terminal);
    @Query("select count(t) > 0 from Trip t where t.driver.id = :driverId and t.status not in :terminal and t.scheduledStartAt < :end and coalesce(t.estimatedArrivalAt, t.scheduledStartAt) > :start")
    boolean hasDriverConflict(@Param("driverId") UUID driverId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("terminal") Collection<TripStatus> terminal);

    /** Đếm chuyến theo trạng thái cho báo cáo thống kê. */
    @Query("select t.status, count(t) from Trip t group by t.status")
    List<Object[]> countByStatus();

    /** Số chuyến khởi hành theo ngày trong khoảng thời gian, phục vụ biểu đồ. */
    @Query("""
            select year(t.scheduledStartAt), month(t.scheduledStartAt), day(t.scheduledStartAt), count(t)
            from Trip t where t.scheduledStartAt >= :from
            group by year(t.scheduledStartAt), month(t.scheduledStartAt), day(t.scheduledStartAt)
            order by year(t.scheduledStartAt), month(t.scheduledStartAt), day(t.scheduledStartAt)""")
    List<Object[]> countByDaySince(@Param("from") LocalDateTime from);

    /** Chuyến đã hoàn tất và về đích không muộn hơn thời điểm dự kiến. */
    @Query("select count(t) from Trip t where t.status = me.xjanua.aitranswatch.enums.TripStatus.COMPLETED and t.actualEndAt is not null and t.estimatedArrivalAt is not null and t.actualEndAt <= t.estimatedArrivalAt")
    long countOnTimeCompleted();

    /** Chuyến đã hoàn tất có đủ dữ liệu để đánh giá đúng hạn. */
    @Query("select count(t) from Trip t where t.status = me.xjanua.aitranswatch.enums.TripStatus.COMPLETED and t.actualEndAt is not null and t.estimatedArrivalAt is not null")
    long countCompletedWithEta();
}
