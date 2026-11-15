package me.xjanua.aitranswatch.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import me.xjanua.aitranswatch.enums.CustomerStatus;
import me.xjanua.aitranswatch.model.Customer;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);

    Page<Customer> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code, Pageable pageable);

    Page<Customer> findByStatusAndNameContainingIgnoreCaseOrStatusAndCodeContainingIgnoreCase(
            CustomerStatus firstStatus, String name, CustomerStatus secondStatus, String code, Pageable pageable);
}
