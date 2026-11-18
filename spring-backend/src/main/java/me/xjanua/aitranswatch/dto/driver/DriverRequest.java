package me.xjanua.aitranswatch.dto.driver;
import java.math.BigDecimal; import java.time.LocalDate; import jakarta.validation.constraints.*; import me.xjanua.aitranswatch.enums.DriverStatus;
public record DriverRequest(@NotBlank @Size(max=50) String employeeCode,@NotBlank @Size(max=100) String licenseNumber,@NotBlank @Size(max=50) String licenseClass,@NotNull @Future LocalDate licenseExpiryDate,@NotNull DriverStatus status,@NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal safetyScore,@NotNull @DecimalMin("0.00") BigDecimal totalDistanceKm) {}
