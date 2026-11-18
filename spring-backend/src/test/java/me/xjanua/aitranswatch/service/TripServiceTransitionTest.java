package me.xjanua.aitranswatch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import me.xjanua.aitranswatch.dto.trip.TripResponse;
import me.xjanua.aitranswatch.enums.DriverStatus;
import me.xjanua.aitranswatch.enums.TripStatus;
import me.xjanua.aitranswatch.enums.VehicleStatus;
import me.xjanua.aitranswatch.model.Driver;
import me.xjanua.aitranswatch.model.Trip;
import me.xjanua.aitranswatch.model.Vehicle;
import me.xjanua.aitranswatch.repository.DriverRepository;
import me.xjanua.aitranswatch.repository.TransportOrderRepository;
import me.xjanua.aitranswatch.repository.TripOrderRepository;
import me.xjanua.aitranswatch.repository.TripRepository;
import me.xjanua.aitranswatch.repository.VehicleRepository;

/**
 * Kiểm thử đơn vị luật chuyển trạng thái chuyến vận chuyển.
 *
 * <p>Không cần cơ sở dữ liệu: repository được mock, chỉ kiểm tra logic nghiệp vụ thuần
 * (trạng thái hợp lệ và việc giải phóng xe/tài xế). Luồng đầy đủ với MySQL được kiểm thử
 * bằng {@code scripts/verify-e2e.ps1}.
 */
@ExtendWith(MockitoExtension.class)
class TripServiceTransitionTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripOrderRepository tripOrderRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private TransportOrderRepository orderRepository;

    private TripService tripService;
    private Vehicle vehicle;
    private Driver driver;
    private final UUID tripId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tripService = new TripService(tripRepository, tripOrderRepository, vehicleRepository, driverRepository, orderRepository);
        vehicle = new Vehicle("51D-000.01", "TRUCK", "Test", "T1", 2024, new BigDecimal("5000"), BigDecimal.ZERO,
                new BigDecimal("80"), VehicleStatus.ON_TRIP, LocalDate.now().plusYears(1), LocalDate.now().plusYears(1),
                new BigDecimal("5000"), new BigDecimal("10000"));
        driver = new Driver("DRV-001", "LIC-001", "C", LocalDate.now().plusYears(1), DriverStatus.ON_TRIP,
                new BigDecimal("95"), BigDecimal.ZERO);
    }

    private Trip tripInStatus(TripStatus status) {
        Trip trip = new Trip("TRIP-TEST-001", vehicle, driver, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(4));
        trip.moveTo(status);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        // Mặc định của Mockito cho phương thức trả về List là danh sách rỗng, đủ để mô phỏng
        // chuyến chưa gắn đơn hàng mà không cần stub thừa.
        return trip;
    }

    @Test
    @DisplayName("SCHEDULED -> PICKUP_IN_PROGRESS hợp lệ và ghi nhận thời điểm bắt đầu")
    void allowsScheduledToPickup() {
        Trip trip = tripInStatus(TripStatus.SCHEDULED);

        TripResponse response = tripService.transition(tripId, TripStatus.PICKUP_IN_PROGRESS);

        assertThat(response.status()).isEqualTo(TripStatus.PICKUP_IN_PROGRESS);
        assertThat(trip.getActualStartAt()).isNotNull();
    }

    @Test
    @DisplayName("IN_TRANSIT -> SCHEDULED bị từ chối với 409")
    void rejectsBackwardTransition() {
        tripInStatus(TripStatus.IN_TRANSIT);

        assertThatThrownBy(() -> tripService.transition(tripId, TripStatus.SCHEDULED))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("PICKUP_IN_PROGRESS -> COMPLETED bị từ chối (phải qua IN_TRANSIT)")
    void rejectsSkippingTransit() {
        tripInStatus(TripStatus.PICKUP_IN_PROGRESS);

        assertThatThrownBy(() -> tripService.transition(tripId, TripStatus.COMPLETED))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Trạng thái kết thúc không thể chuyển tiếp")
    void terminalStatusIsFinal() {
        tripInStatus(TripStatus.COMPLETED);

        assertThatThrownBy(() -> tripService.transition(tripId, TripStatus.IN_TRANSIT))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("Hoàn tất chuyến giải phóng xe và tài xế về AVAILABLE")
    void completesAndReleasesResources() {
        tripInStatus(TripStatus.IN_TRANSIT);

        tripService.transition(tripId, TripStatus.COMPLETED);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(driver.getStatus()).isEqualTo(DriverStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Sự cố có thể quay lại IN_TRANSIT")
    void incidentCanResume() {
        tripInStatus(TripStatus.INCIDENT);

        assertThat(tripService.transition(tripId, TripStatus.IN_TRANSIT).status()).isEqualTo(TripStatus.IN_TRANSIT);
    }
}
