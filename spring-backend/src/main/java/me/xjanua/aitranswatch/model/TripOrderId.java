package me.xjanua.aitranswatch.model;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class TripOrderId implements Serializable {
    @Column(name = "trip_id", columnDefinition = "BINARY(16)")
    private UUID tripId;
    @Column(name = "transport_order_id", columnDefinition = "BINARY(16)")
    private UUID transportOrderId;
    public TripOrderId(UUID tripId, UUID transportOrderId) { this.tripId = tripId; this.transportOrderId = transportOrderId; }
}
