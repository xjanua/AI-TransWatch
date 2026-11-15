package me.xjanua.aitranswatch.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.xjanua.aitranswatch.enums.DriverStatus;

@Entity
@Table(name = "drivers")
@Getter
@NoArgsConstructor
public class Driver {
    @Id @UuidGenerator @JdbcTypeCode(SqlTypes.BINARY) private UUID id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") @JdbcTypeCode(SqlTypes.BINARY) private User user;
    @Column(name = "employee_code", nullable = false, unique = true, length = 50) private String employeeCode;
    @Column(name = "license_number", nullable = false, unique = true, length = 100) private String licenseNumber;
    @Column(name = "license_class", nullable = false, length = 50) private String licenseClass;
    @Column(name = "license_expiry_date", nullable = false) private LocalDate licenseExpiryDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private DriverStatus status;
    @Column(name = "safety_score", nullable = false, precision = 5, scale = 2) private BigDecimal safetyScore;
    @Column(name = "total_distance_km", nullable = false, precision = 12, scale = 2) private BigDecimal totalDistanceKm;
    public Driver(String employeeCode, String licenseNumber, String licenseClass, LocalDate licenseExpiryDate, DriverStatus status, BigDecimal safetyScore, BigDecimal totalDistanceKm) { apply(employeeCode, licenseNumber, licenseClass, licenseExpiryDate, status, safetyScore, totalDistanceKm); }
    public void update(String employeeCode, String licenseNumber, String licenseClass, LocalDate licenseExpiryDate, DriverStatus status, BigDecimal safetyScore, BigDecimal totalDistanceKm) { apply(employeeCode, licenseNumber, licenseClass, licenseExpiryDate, status, safetyScore, totalDistanceKm); }
    private void apply(String employeeCode, String licenseNumber, String licenseClass, LocalDate licenseExpiryDate, DriverStatus status, BigDecimal safetyScore, BigDecimal totalDistanceKm) { this.employeeCode=employeeCode; this.licenseNumber=licenseNumber; this.licenseClass=licenseClass; this.licenseExpiryDate=licenseExpiryDate; this.status=status; this.safetyScore=safetyScore; this.totalDistanceKm=totalDistanceKm; }
    public void markOnTrip() { this.status = DriverStatus.ON_TRIP; }
    public void markAvailable() { this.status = DriverStatus.AVAILABLE; }
    public void suspend() { this.status=DriverStatus.SUSPENDED; }
}
