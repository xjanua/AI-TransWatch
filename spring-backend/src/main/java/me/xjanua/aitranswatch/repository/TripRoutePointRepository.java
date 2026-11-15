package me.xjanua.aitranswatch.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import me.xjanua.aitranswatch.model.TripRoutePoint;

public interface TripRoutePointRepository extends JpaRepository<TripRoutePoint, UUID> {
    List<TripRoutePoint> findByTripIdOrderBySequenceNo(UUID tripId);
}
