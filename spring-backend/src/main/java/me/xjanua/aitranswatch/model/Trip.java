package me.xjanua.aitranswatch.model;

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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.xjanua.aitranswatch.enums.TripStatus;

@Entity
@Table(name = "trips")
@Getter
@NoArgsConstructor
public class Trip {
    @Id @UuidGenerator @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;
    @Column(name = "trip_code", nullable = false, unique = true, length = 50)
    private String tripCode;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private TripStatus status;
    @Column(name = "scheduled_start_at", nullable = false)
    private LocalDateTime scheduledStartAt;
    @Column(name = "actual_start_at") private LocalDateTime actualStartAt;
    @Column(name = "estimated_arrival_at") private LocalDateTime estimatedArrivalAt;
    @Column(name = "actual_end_at") private LocalDateTime actualEndAt;
    @Version private Long version;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) private LocalDateTime updatedAt;

    public Trip(String tripCode, Vehicle vehicle, Driver driver, LocalDateTime scheduledStartAt, LocalDateTime estimatedArrivalAt) {
        this.tripCode = tripCode;
        this.vehicle = vehicle;
        this.driver = driver;
        this.status = TripStatus.SCHEDULED;
        this.scheduledStartAt = scheduledStartAt;
        this.estimatedArrivalAt = estimatedArrivalAt;
    }
    public void moveTo(TripStatus newStatus) {
        this.status = newStatus;
        if (newStatus == TripStatus.PICKUP_IN_PROGRESS && actualStartAt == null) actualStartAt = LocalDateTime.now();
        if ((newStatus == TripStatus.COMPLETED || newStatus == TripStatus.CANCELLED) && actualEndAt == null) actualEndAt = LocalDateTime.now();
    }
}
