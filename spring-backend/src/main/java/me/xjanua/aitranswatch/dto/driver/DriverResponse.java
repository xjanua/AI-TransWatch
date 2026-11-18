package me.xjanua.aitranswatch.dto.driver;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID; import me.xjanua.aitranswatch.enums.DriverStatus;
public record DriverResponse(UUID id,String employeeCode,String licenseNumber,String licenseClass,LocalDate licenseExpiryDate,DriverStatus status,BigDecimal safetyScore,BigDecimal totalDistanceKm) {}
