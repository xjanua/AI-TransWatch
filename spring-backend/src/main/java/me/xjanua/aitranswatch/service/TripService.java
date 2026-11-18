package me.xjanua.aitranswatch.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import me.xjanua.aitranswatch.dto.trip.TripCreateRequest;
import me.xjanua.aitranswatch.dto.trip.TripOrderResponse;
import me.xjanua.aitranswatch.dto.trip.TripResponse;
import me.xjanua.aitranswatch.enums.DriverStatus;
import me.xjanua.aitranswatch.enums.OrderStatus;
import me.xjanua.aitranswatch.enums.TripStatus;
import me.xjanua.aitranswatch.enums.VehicleStatus;
import me.xjanua.aitranswatch.model.Driver;
import me.xjanua.aitranswatch.model.TransportOrder;
import me.xjanua.aitranswatch.model.Trip;
import me.xjanua.aitranswatch.model.TripOrder;
import me.xjanua.aitranswatch.model.Vehicle;
import me.xjanua.aitranswatch.repository.DriverRepository;
import me.xjanua.aitranswatch.repository.TransportOrderRepository;
import me.xjanua.aitranswatch.repository.TripOrderRepository;
import me.xjanua.aitranswatch.repository.TripRepository;
import me.xjanua.aitranswatch.repository.VehicleRepository;

@Service
@Transactional(readOnly = true)
public class TripService {
    private static final EnumSet<TripStatus> TERMINAL = EnumSet.of(TripStatus.COMPLETED, TripStatus.CANCELLED);
    private final TripRepository tripRepository;
    private final TripOrderRepository tripOrderRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TransportOrderRepository orderRepository;

    public TripService(TripRepository tripRepository, TripOrderRepository tripOrderRepository, VehicleRepository vehicleRepository,
            DriverRepository driverRepository, TransportOrderRepository orderRepository) {
        this.tripRepository = tripRepository; this.tripOrderRepository = tripOrderRepository; this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository; this.orderRepository = orderRepository;
    }

    public Page<TripResponse> findAll(int page, int size, TripStatus status, UUID vehicleId, UUID driverId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("scheduledStartAt").descending());
        Page<Trip> trips;
        if (status != null && vehicleId != null) trips = tripRepository.findByStatusAndVehicleId(status, vehicleId, pageable);
        else if (status != null && driverId != null) trips = tripRepository.findByStatusAndDriverId(status, driverId, pageable);
        else if (status != null) trips = tripRepository.findByStatus(status, pageable);
        else if (vehicleId != null) trips = tripRepository.findByVehicleId(vehicleId, pageable);
        else if (driverId != null) trips = tripRepository.findByDriverId(driverId, pageable);
        else trips = tripRepository.findAll(pageable);
        return trips.map(this::toResponse);
    }

    public TripResponse findById(UUID id) { return toResponse(getTrip(id)); }

    @Transactional
    public TripResponse create(TripCreateRequest request) {
        if (request.estimatedArrivalAt() != null && !request.estimatedArrivalAt().isAfter(request.scheduledStartAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời điểm đến dự kiến phải sau thời điểm bắt đầu");
        }
        String tripCode = request.tripCode().trim();
        if (tripRepository.existsByTripCodeIgnoreCase(tripCode)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã chuyến đã tồn tại");
        if (request.orderIds().stream().distinct().count() != request.orderIds().size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách đơn hàng bị trùng");
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy xe"));
        Driver driver = driverRepository.findById(request.driverId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài xế"));
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) throw new ResponseStatusException(HttpStatus.CONFLICT, "Xe không sẵn sàng điều phối");
        if (driver.getStatus() != DriverStatus.AVAILABLE) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tài xế không sẵn sàng điều phối");
        LocalDateTime end = request.estimatedArrivalAt() == null ? request.scheduledStartAt().plusHours(8) : request.estimatedArrivalAt();
        if (tripRepository.hasVehicleConflict(vehicle.getId(), request.scheduledStartAt(), end, TERMINAL) || tripRepository.hasDriverConflict(driver.getId(), request.scheduledStartAt(), end, TERMINAL)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Xe hoặc tài xế đã có chuyến trùng lịch");
        }
        List<TransportOrder> orders = orderRepository.findAllById(request.orderIds());
        if (orders.size() != request.orderIds().size()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Có đơn hàng không tồn tại");
        if (orders.stream().anyMatch(order -> order.getStatus() != OrderStatus.PENDING)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ được phân công đơn đang chờ điều phối");
        BigDecimal totalWeight = orders.stream().map(TransportOrder::getWeightKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(vehicle.getMaxPayloadKg()) > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tổng khối lượng đơn vượt tải trọng xe");
        Trip trip = tripRepository.save(new Trip(tripCode, vehicle, driver, request.scheduledStartAt(), request.estimatedArrivalAt()));
        for (int index = 0; index < request.orderIds().size(); index++) {
            UUID orderId = request.orderIds().get(index);
            TransportOrder order = orders.stream().filter(item -> item.getId().equals(orderId)).findFirst().orElseThrow();
            order.assign();
            tripOrderRepository.save(new TripOrder(trip, order, index + 1));
        }
        vehicle.markOnTrip(); driver.markOnTrip();
        return toResponse(trip);
    }

    @Transactional
    public TripResponse transition(UUID id, TripStatus target) {
        Trip trip = getTrip(id); TripStatus current = trip.getStatus();
        if (!isLegalTransition(current, target)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Chuyển trạng thái chuyến không hợp lệ: " + current + " -> " + target);
        List<TripOrder> links = tripOrderRepository.findByTripIdOrderBySequenceNo(id);
        if (target == TripStatus.IN_TRANSIT) links.forEach(link -> link.getTransportOrder().markInTransit());
        if (target == TripStatus.COMPLETED) { links.forEach(link -> link.getTransportOrder().deliver()); releaseResources(trip); }
        if (target == TripStatus.CANCELLED) { links.stream().map(TripOrder::getTransportOrder).filter(order -> order.getStatus() == OrderStatus.ASSIGNED).forEach(TransportOrder::restorePending); releaseResources(trip); }
        trip.moveTo(target);
        return toResponse(trip);
    }

    private boolean isLegalTransition(TripStatus from, TripStatus to) {
        if (from == to || TERMINAL.contains(from)) return false;
        return switch (from) {
            case SCHEDULED -> to == TripStatus.PICKUP_IN_PROGRESS || to == TripStatus.CANCELLED;
            case PICKUP_IN_PROGRESS -> to == TripStatus.IN_TRANSIT || to == TripStatus.INCIDENT || to == TripStatus.CANCELLED;
            case IN_TRANSIT -> to == TripStatus.COMPLETED || to == TripStatus.INCIDENT;
            case INCIDENT -> to == TripStatus.IN_TRANSIT || to == TripStatus.CANCELLED;
            default -> false;
        };
    }
    private void releaseResources(Trip trip) { trip.getVehicle().markAvailable(); trip.getDriver().markAvailable(); }
    private Trip getTrip(UUID id) { return tripRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chuyến vận chuyển")); }
    private TripResponse toResponse(Trip trip) {
        List<TripOrderResponse> orders = tripOrderRepository.findByTripIdOrderBySequenceNo(trip.getId()).stream().map(link -> {
            TransportOrder order = link.getTransportOrder(); return new TripOrderResponse(order.getId(), order.getOrderCode(), order.getCargoName(), order.getWeightKg(), order.getStatus(), link.getSequenceNo());
        }).toList();
        return new TripResponse(trip.getId(), trip.getTripCode(), trip.getStatus(), trip.getVehicle().getId(), trip.getVehicle().getLicensePlate(), trip.getDriver().getId(), trip.getDriver().getEmployeeCode(), trip.getScheduledStartAt(), trip.getActualStartAt(), trip.getEstimatedArrivalAt(), trip.getActualEndAt(), orders);
    }
}
