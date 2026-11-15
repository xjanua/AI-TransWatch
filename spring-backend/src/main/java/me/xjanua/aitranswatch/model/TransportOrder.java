package me.xjanua.aitranswatch.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.xjanua.aitranswatch.enums.OrderPriority;
import me.xjanua.aitranswatch.enums.OrderStatus;

@Entity
@Table(name = "transport_orders")
@Getter
@NoArgsConstructor
public class TransportOrder {
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderPriority priority;

    @Column(name = "cargo_name", nullable = false, length = 255)
    private String cargoName;

    @Column(name = "weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "pickup_address", nullable = false, length = 500)
    private String pickupAddress;

    @Column(name = "pickup_latitude", precision = 10, scale = 7)
    private BigDecimal pickupLatitude;

    @Column(name = "pickup_longitude", precision = 10, scale = 7)
    private BigDecimal pickupLongitude;

    @Column(name = "delivery_address", nullable = false, length = 500)
    private String deliveryAddress;

    @Column(name = "delivery_latitude", precision = 10, scale = 7)
    private BigDecimal deliveryLatitude;

    @Column(name = "delivery_longitude", precision = 10, scale = 7)
    private BigDecimal deliveryLongitude;

    @Column(name = "delivery_deadline")
    private LocalDateTime deliveryDeadline;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public TransportOrder(String orderCode, Customer customer, OrderStatus status, OrderPriority priority,
            String cargoName, BigDecimal weightKg, String pickupAddress, BigDecimal pickupLatitude,
            BigDecimal pickupLongitude, String deliveryAddress, BigDecimal deliveryLatitude,
            BigDecimal deliveryLongitude, LocalDateTime deliveryDeadline) {
        apply(orderCode, customer, status, priority, cargoName, weightKg, pickupAddress, pickupLatitude,
                pickupLongitude, deliveryAddress, deliveryLatitude, deliveryLongitude, deliveryDeadline);
    }

    public void update(String orderCode, Customer customer, OrderStatus status, OrderPriority priority,
            String cargoName, BigDecimal weightKg, String pickupAddress, BigDecimal pickupLatitude,
            BigDecimal pickupLongitude, String deliveryAddress, BigDecimal deliveryLatitude,
            BigDecimal deliveryLongitude, LocalDateTime deliveryDeadline) {
        apply(orderCode, customer, status, priority, cargoName, weightKg, pickupAddress, pickupLatitude,
                pickupLongitude, deliveryAddress, deliveryLatitude, deliveryLongitude, deliveryDeadline);
    }

    public void assign() { this.status = OrderStatus.ASSIGNED; }
    public void markInTransit() { this.status = OrderStatus.IN_TRANSIT; }
    public void deliver() { this.status = OrderStatus.DELIVERED; }
    public void restorePending() { this.status = OrderStatus.PENDING; }
    public void cancel() { this.status = OrderStatus.CANCELLED; }

    private void apply(String orderCode, Customer customer, OrderStatus status, OrderPriority priority,
            String cargoName, BigDecimal weightKg, String pickupAddress, BigDecimal pickupLatitude,
            BigDecimal pickupLongitude, String deliveryAddress, BigDecimal deliveryLatitude,
            BigDecimal deliveryLongitude, LocalDateTime deliveryDeadline) {
        this.orderCode = orderCode; this.customer = customer; this.status = status; this.priority = priority;
        this.cargoName = cargoName; this.weightKg = weightKg; this.pickupAddress = pickupAddress;
        this.pickupLatitude = pickupLatitude; this.pickupLongitude = pickupLongitude;
        this.deliveryAddress = deliveryAddress; this.deliveryLatitude = deliveryLatitude;
        this.deliveryLongitude = deliveryLongitude; this.deliveryDeadline = deliveryDeadline;
    }
}
