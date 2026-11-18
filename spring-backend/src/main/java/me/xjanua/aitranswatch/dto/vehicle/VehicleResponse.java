package me.xjanua.aitranswatch.dto.vehicle;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID; import me.xjanua.aitranswatch.enums.VehicleStatus;
public record VehicleResponse(UUID id,String licensePlate,String vehicleType,String brand,String model,Integer manufactureYear,BigDecimal maxPayloadKg,BigDecimal currentOdometerKm,BigDecimal fuelLevelPercent,VehicleStatus status,LocalDate registrationExpiryDate,LocalDate insuranceExpiryDate,BigDecimal maintenanceIntervalKm,BigDecimal nextMaintenanceOdometerKm) {}
