package me.xjanua.aitranswatch.model;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.xjanua.aitranswatch.enums.AlertSeverity;
import me.xjanua.aitranswatch.enums.AlertStatus;
import me.xjanua.aitranswatch.enums.AlertType;

@Entity
@Table(name = "alerts")
@Getter
@NoArgsConstructor
public class Alert {
    @Id @UuidGenerator @JdbcTypeCode(SqlTypes.BINARY) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "trip_id") private Trip trip;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "vehicle_id") private Vehicle vehicle;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "driver_id") private Driver driver;
    @Enumerated(EnumType.STRING) @Column(name = "alert_type", nullable = false) private AlertType alertType;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AlertSeverity severity;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AlertStatus status;
    @Column(columnDefinition = "json") private String evidence;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "handled_by") private User handledBy;
    @Column(name = "handled_at") private LocalDateTime handledAt;
    @Column(name = "handling_note", length = 1000) private String handlingNote;
    public Alert(Trip trip, AlertType alertType, AlertSeverity severity, String evidence, LocalDateTime occurredAt) {
        this.trip = trip; this.vehicle = trip.getVehicle(); this.driver = trip.getDriver(); this.alertType = alertType;
        this.severity = severity; this.status = AlertStatus.NEW; this.evidence = evidence; this.occurredAt = occurredAt;
    }
    /** Cảnh báo không gắn chuyến: hết hạn giấy tờ xe, đến hạn bảo dưỡng, hết hạn giấy phép lái xe. */
    public Alert(Vehicle vehicle, Driver driver, AlertType alertType, AlertSeverity severity, String evidence, LocalDateTime occurredAt) {
        this.vehicle = vehicle; this.driver = driver; this.alertType = alertType;
        this.severity = severity; this.status = AlertStatus.NEW; this.evidence = evidence; this.occurredAt = occurredAt;
    }
    public void updateStatus(AlertStatus status, User handledBy, String note) {
        this.status = status; this.handledBy = handledBy; this.handlingNote = note;
        if (status == AlertStatus.RESOLVED || status == AlertStatus.DISMISSED) this.handledAt = LocalDateTime.now();
    }
}
