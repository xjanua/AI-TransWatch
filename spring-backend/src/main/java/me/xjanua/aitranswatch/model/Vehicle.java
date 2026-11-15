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
import me.xjanua.aitranswatch.enums.VehicleStatus;

@Entity
@Table(name = "vehicles")
@Getter
@NoArgsConstructor
public class Vehicle {
    @Id @UuidGenerator @JdbcTypeCode(SqlTypes.BINARY) private UUID id;
    @Column(name = "license_plate", nullable = false, unique = true, length = 30) private String licensePlate;
    @Column(name = "vehicle_type", nullable = false, length = 50) private String vehicleType;
    @Column(length = 100) private String brand;
    @Column(length = 100) private String model;
    @Column(name = "manufacture_year") private Integer manufactureYear;
    @Column(name = "max_payload_kg", nullable = false, precision = 12, scale = 2) private BigDecimal maxPayloadKg;
    @Column(name = "current_odometer_km", nullable = false, precision = 12, scale = 2) private BigDecimal currentOdometerKm;
    @Column(name = "fuel_level_percent", precision = 5, scale = 2) private BigDecimal fuelLevelPercent;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private VehicleStatus status;
    @Column(name = "registration_expiry_date") private LocalDate registrationExpiryDate;
    @Column(name = "insurance_expiry_date") private LocalDate insuranceExpiryDate;
    @Column(name = "maintenance_interval_km", precision = 12, scale = 2) private BigDecimal maintenanceIntervalKm;
    @Column(name = "next_maintenance_odometer_km", precision = 12, scale = 2) private BigDecimal nextMaintenanceOdometerKm;

    public Vehicle(String licensePlate, String vehicleType, String brand, String model, Integer manufactureYear, BigDecimal maxPayloadKg, BigDecimal currentOdometerKm, BigDecimal fuelLevelPercent, VehicleStatus status, LocalDate registrationExpiryDate, LocalDate insuranceExpiryDate, BigDecimal maintenanceIntervalKm, BigDecimal nextMaintenanceOdometerKm) {
        apply(licensePlate, vehicleType, brand, model, manufactureYear, maxPayloadKg, currentOdometerKm, fuelLevelPercent, status, registrationExpiryDate, insuranceExpiryDate, maintenanceIntervalKm, nextMaintenanceOdometerKm);
    }
    public void update(String licensePlate, String vehicleType, String brand, String model, Integer manufactureYear, BigDecimal maxPayloadKg, BigDecimal currentOdometerKm, BigDecimal fuelLevelPercent, VehicleStatus status, LocalDate registrationExpiryDate, LocalDate insuranceExpiryDate, BigDecimal maintenanceIntervalKm, BigDecimal nextMaintenanceOdometerKm) { apply(licensePlate, vehicleType, brand, model, manufactureYear, maxPayloadKg, currentOdometerKm, fuelLevelPercent, status, registrationExpiryDate, insuranceExpiryDate, maintenanceIntervalKm, nextMaintenanceOdometerKm); }
    private void apply(String licensePlate, String vehicleType, String brand, String model, Integer manufactureYear, BigDecimal maxPayloadKg, BigDecimal currentOdometerKm, BigDecimal fuelLevelPercent, VehicleStatus status, LocalDate registrationExpiryDate, LocalDate insuranceExpiryDate, BigDecimal maintenanceIntervalKm, BigDecimal nextMaintenanceOdometerKm) {
        this.licensePlate=licensePlate; this.vehicleType=vehicleType; this.brand=brand; this.model=model; this.manufactureYear=manufactureYear; this.maxPayloadKg=maxPayloadKg; this.currentOdometerKm=currentOdometerKm; this.fuelLevelPercent=fuelLevelPercent; this.status=status; this.registrationExpiryDate=registrationExpiryDate; this.insuranceExpiryDate=insuranceExpiryDate; this.maintenanceIntervalKm=maintenanceIntervalKm; this.nextMaintenanceOdometerKm=nextMaintenanceOdometerKm;
    }
    public void markOnTrip() { this.status = VehicleStatus.ON_TRIP; }
    public void markAvailable() { this.status = VehicleStatus.AVAILABLE; }
    public void deactivate() { this.status = VehicleStatus.INACTIVE; }
}
