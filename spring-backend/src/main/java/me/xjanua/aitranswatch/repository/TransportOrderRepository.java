package me.xjanua.aitranswatch.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import me.xjanua.aitranswatch.enums.OrderStatus;
import me.xjanua.aitranswatch.model.TransportOrder;

public interface TransportOrderRepository extends JpaRepository<TransportOrder, UUID> {
    boolean existsByOrderCodeIgnoreCase(String orderCode);
    boolean existsByOrderCodeIgnoreCaseAndIdNot(String orderCode, UUID id);

    Page<TransportOrder> findByStatus(OrderStatus status, Pageable pageable);
    Page<TransportOrder> findByCustomerId(UUID customerId, Pageable pageable);
    Page<TransportOrder> findByCustomerIdAndStatus(UUID customerId, OrderStatus status, Pageable pageable);
    Page<TransportOrder> findByOrderCodeContainingIgnoreCaseOrCargoNameContainingIgnoreCase(
            String orderCode, String cargoName, Pageable pageable);
    Page<TransportOrder> findByStatusAndOrderCodeContainingIgnoreCaseOrStatusAndCargoNameContainingIgnoreCase(
            OrderStatus firstStatus, String orderCode, OrderStatus secondStatus, String cargoName, Pageable pageable);
    Page<TransportOrder> findByCustomerIdAndOrderCodeContainingIgnoreCaseOrCustomerIdAndCargoNameContainingIgnoreCase(
            UUID firstCustomerId, String orderCode, UUID secondCustomerId, String cargoName, Pageable pageable);
    Page<TransportOrder> findByCustomerIdAndStatusAndOrderCodeContainingIgnoreCaseOrCustomerIdAndStatusAndCargoNameContainingIgnoreCase(
            UUID firstCustomerId, OrderStatus firstStatus, String orderCode, UUID secondCustomerId, OrderStatus secondStatus, String cargoName, Pageable pageable);

    /** Số đơn theo trạng thái cho báo cáo. */
    @org.springframework.data.jpa.repository.Query("select o.status, count(o) from TransportOrder o group by o.status")
    java.util.List<Object[]> countByStatus();
}
