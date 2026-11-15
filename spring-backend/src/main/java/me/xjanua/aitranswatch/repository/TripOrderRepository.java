package me.xjanua.aitranswatch.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import me.xjanua.aitranswatch.model.TripOrder;
import me.xjanua.aitranswatch.model.TripOrderId;

public interface TripOrderRepository extends JpaRepository<TripOrder, TripOrderId> {
    List<TripOrder> findByTripIdOrderBySequenceNo(UUID tripId);
}
