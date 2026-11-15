package me.xjanua.aitranswatch.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gps_logs")
@Getter
@NoArgsConstructor
public class GpsLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "trip_id", nullable = false) private Trip trip;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "vehicle_id", nullable = false) private Vehicle vehicle;
    @Column(nullable = false, precision = 10, scale = 7) private BigDecimal latitude;
    @Column(nullable = false, precision = 10, scale = 7) private BigDecimal longitude;
    @Column(name = "speed_kmh", nullable = false, precision = 6, scale = 2) private BigDecimal speedKmh;
    @Column(name = "heading_degree", precision = 6, scale = 2) private BigDecimal headingDegree;
    @Column(name = "engine_on", nullable = false) private boolean engineOn;
    @Column(name = "stopped_minutes") private Integer stoppedMinutes;
    @Column(name = "recorded_at", nullable = false) private LocalDateTime recordedAt;
    public GpsLog(Trip trip, BigDecimal latitude, BigDecimal longitude, BigDecimal speedKmh, BigDecimal headingDegree, boolean engineOn, LocalDateTime recordedAt) {
        this(trip, latitude, longitude, speedKmh, headingDegree, engineOn, null, recordedAt);
    }
    public GpsLog(Trip trip, BigDecimal latitude, BigDecimal longitude, BigDecimal speedKmh, BigDecimal headingDegree,
            boolean engineOn, Integer stoppedMinutes, LocalDateTime recordedAt) {
        this.trip = trip; this.vehicle = trip.getVehicle(); this.latitude = latitude; this.longitude = longitude;
        this.speedKmh = speedKmh; this.headingDegree = headingDegree; this.engineOn = engineOn;
        this.stoppedMinutes = stoppedMinutes; this.recordedAt = recordedAt;
    }
}
