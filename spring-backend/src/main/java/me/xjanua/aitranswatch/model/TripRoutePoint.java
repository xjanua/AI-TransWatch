package me.xjanua.aitranswatch.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.xjanua.aitranswatch.enums.RoutePointType;

/** Điểm lộ trình dự kiến của chuyến, dùng để phát hiện xe đi lệch tuyến. */
@Entity
@Table(name = "trip_route_points")
@Getter
@NoArgsConstructor
public class TripRoutePoint {
    @Id @UuidGenerator @JdbcTypeCode(SqlTypes.BINARY) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "trip_id", nullable = false) private Trip trip;
    @Column(name = "sequence_no", nullable = false) private int sequenceNo;
    @Enumerated(EnumType.STRING) @Column(name = "point_type", nullable = false, length = 30) private RoutePointType pointType;
    @Column(nullable = false, precision = 10, scale = 7) private BigDecimal latitude;
    @Column(nullable = false, precision = 10, scale = 7) private BigDecimal longitude;
    @Column(name = "allowed_radius_m", precision = 10, scale = 2) private BigDecimal allowedRadiusM;
    @Column(name = "planned_arrival_at") private LocalDateTime plannedArrivalAt;

    public TripRoutePoint(Trip trip, int sequenceNo, RoutePointType pointType, BigDecimal latitude, BigDecimal longitude,
            BigDecimal allowedRadiusM, LocalDateTime plannedArrivalAt) {
        this.trip = trip;
        this.sequenceNo = sequenceNo;
        this.pointType = pointType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowedRadiusM = allowedRadiusM;
        this.plannedArrivalAt = plannedArrivalAt;
    }
}
