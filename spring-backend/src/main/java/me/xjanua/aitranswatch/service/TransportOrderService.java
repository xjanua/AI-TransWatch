package me.xjanua.aitranswatch.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import me.xjanua.aitranswatch.dto.order.TransportOrderPageResponse;
import me.xjanua.aitranswatch.dto.order.TransportOrderRequest;
import me.xjanua.aitranswatch.dto.order.TransportOrderResponse;
import me.xjanua.aitranswatch.enums.CustomerStatus;
import me.xjanua.aitranswatch.enums.OrderPriority;
import me.xjanua.aitranswatch.enums.OrderStatus;
import me.xjanua.aitranswatch.model.Customer;
import me.xjanua.aitranswatch.model.TransportOrder;
import me.xjanua.aitranswatch.repository.CustomerRepository;
import me.xjanua.aitranswatch.repository.TransportOrderRepository;

@Service
@Transactional(readOnly = true)
public class TransportOrderService {
    private final TransportOrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public TransportOrderService(TransportOrderRepository orderRepository, CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    public TransportOrderPageResponse findAll(int page, int size, String search, OrderStatus status, UUID customerId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String keyword = search == null ? null : search.trim();
        boolean hasSearch = keyword != null && !keyword.isBlank();
        Page<TransportOrder> orders;
        if (customerId != null && status != null && hasSearch) {
            orders = orderRepository.findByCustomerIdAndStatusAndOrderCodeContainingIgnoreCaseOrCustomerIdAndStatusAndCargoNameContainingIgnoreCase(
                    customerId, status, keyword, customerId, status, keyword, pageable);
        } else if (customerId != null && status != null) {
            orders = orderRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        } else if (customerId != null && hasSearch) {
            orders = orderRepository.findByCustomerIdAndOrderCodeContainingIgnoreCaseOrCustomerIdAndCargoNameContainingIgnoreCase(
                    customerId, keyword, customerId, keyword, pageable);
        } else if (status != null && hasSearch) {
            orders = orderRepository.findByStatusAndOrderCodeContainingIgnoreCaseOrStatusAndCargoNameContainingIgnoreCase(
                    status, keyword, status, keyword, pageable);
        } else if (customerId != null) {
            orders = orderRepository.findByCustomerId(customerId, pageable);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status, pageable);
        } else if (hasSearch) {
            orders = orderRepository.findByOrderCodeContainingIgnoreCaseOrCargoNameContainingIgnoreCase(keyword, keyword, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }
        return new TransportOrderPageResponse(orders.getContent().stream().map(this::toResponse).toList(),
                orders.getNumber(), orders.getSize(), orders.getTotalElements(), orders.getTotalPages());
    }

    public TransportOrderResponse findById(UUID id) { return toResponse(getOrder(id)); }

    @Transactional
    public TransportOrderResponse create(TransportOrderRequest request) {
        String orderCode = request.orderCode().trim();
        if (orderRepository.existsByOrderCodeIgnoreCase(orderCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã đơn vận chuyển đã tồn tại");
        }
        validateLocationPairs(request);
        Customer customer = getActiveCustomer(request.customerId());
        TransportOrder order = new TransportOrder(orderCode, customer,
                request.status() == null ? OrderStatus.PENDING : request.status(),
                request.priority() == null ? OrderPriority.NORMAL : request.priority(), request.cargoName().trim(),
                request.weightKg(), request.pickupAddress().trim(), request.pickupLatitude(), request.pickupLongitude(),
                request.deliveryAddress().trim(), request.deliveryLatitude(), request.deliveryLongitude(), request.deliveryDeadline());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public TransportOrderResponse update(UUID id, TransportOrderRequest request) {
        TransportOrder order = getOrder(id);
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể cập nhật đơn đã hoàn thành hoặc đã hủy");
        }
        String orderCode = request.orderCode().trim();
        if (orderRepository.existsByOrderCodeIgnoreCaseAndIdNot(orderCode, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã đơn vận chuyển đã tồn tại");
        }
        validateLocationPairs(request);
        Customer customer = getActiveCustomer(request.customerId());
        order.update(orderCode, customer, request.status() == null ? order.getStatus() : request.status(),
                request.priority() == null ? order.getPriority() : request.priority(), request.cargoName().trim(),
                request.weightKg(), request.pickupAddress().trim(), request.pickupLatitude(), request.pickupLongitude(),
                request.deliveryAddress().trim(), request.deliveryLatitude(), request.deliveryLongitude(), request.deliveryDeadline());
        return toResponse(order);
    }

    @Transactional
    public void cancel(UUID id) {
        TransportOrder order = getOrder(id);
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể hủy đơn đã giao thành công");
        }
        order.cancel();
    }

    private Customer getActiveCustomer(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khách hàng không còn hoạt động");
        }
        return customer;
    }

    private TransportOrder getOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn vận chuyển"));
    }

    private void validateLocationPairs(TransportOrderRequest request) {
        validateCoordinatePair(request.pickupLatitude(), request.pickupLongitude(), "điểm lấy hàng");
        validateCoordinatePair(request.deliveryLatitude(), request.deliveryLongitude(), "điểm giao hàng");
        if (request.deliveryDeadline() != null && request.deliveryDeadline().isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn giao hàng không được ở quá khứ");
        }
    }

    private void validateCoordinatePair(BigDecimal latitude, BigDecimal longitude, String location) {
        if ((latitude == null) != (longitude == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vĩ độ và kinh độ " + location + " phải được cung cấp cùng nhau");
        }
        if (latitude != null && (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vĩ độ " + location + " phải trong khoảng -90 đến 90");
        }
        if (longitude != null && (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kinh độ " + location + " phải trong khoảng -180 đến 180");
        }
    }

    private TransportOrderResponse toResponse(TransportOrder order) {
        Customer customer = order.getCustomer();
        return new TransportOrderResponse(order.getId(), order.getOrderCode(), customer.getId(), customer.getCode(), customer.getName(),
                order.getStatus(), order.getPriority(), order.getCargoName(), order.getWeightKg(), order.getPickupAddress(),
                order.getPickupLatitude(), order.getPickupLongitude(), order.getDeliveryAddress(), order.getDeliveryLatitude(),
                order.getDeliveryLongitude(), order.getDeliveryDeadline(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
