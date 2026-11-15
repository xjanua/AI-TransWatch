package me.xjanua.aitranswatch.model;

import java.time.LocalDateTime;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trip_orders")
@Getter
@NoArgsConstructor
public class TripOrder {
    @EmbeddedId private TripOrderId id;
    @MapsId("tripId") @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "trip_id") private Trip trip;
    @MapsId("transportOrderId") @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "transport_order_id") private TransportOrder transportOrder;
    private int sequenceNo;
    private LocalDateTime assignedAt;
    public TripOrder(Trip trip, TransportOrder transportOrder, int sequenceNo) {
        this.id = new TripOrderId(trip.getId(), transportOrder.getId()); this.trip = trip; this.transportOrder = transportOrder;
        this.sequenceNo = sequenceNo; this.assignedAt = LocalDateTime.now();
    }
}
