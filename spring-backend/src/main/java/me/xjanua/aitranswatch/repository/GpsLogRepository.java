package me.xjanua.aitranswatch.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import me.xjanua.aitranswatch.model.GpsLog;

public interface GpsLogRepository extends JpaRepository<GpsLog, Long> {
    Optional<GpsLog> findFirstByTripIdOrderByRecordedAtDesc(UUID tripId);

    /** Tốc độ trung bình đã ghi nhận của chuyến; dùng làm đặc trưng lịch sử cho mô hình AI. */
    @Query("select avg(g.speedKmh) from GpsLog g where g.trip.id = :tripId")
    BigDecimal averageSpeedKmhByTripId(@Param("tripId") UUID tripId);

    /** Bản ghi GPS sớm nhất không muộn hơn mốc thời gian (mốc bắt đầu ca lái). */
    Optional<GpsLog> findFirstByTripIdAndRecordedAtLessThanEqualOrderByRecordedAtAsc(UUID tripId, java.time.LocalDateTime at);

    /** Lần dừng nghỉ đủ dài gần nhất trước mốc thời gian (mốc kết thúc ca lái trước đó). */
    Optional<GpsLog> findFirstByTripIdAndStoppedMinutesGreaterThanEqualAndRecordedAtLessThanEqualOrderByRecordedAtDesc(
            UUID tripId, Integer minutes, java.time.LocalDateTime at);
}
