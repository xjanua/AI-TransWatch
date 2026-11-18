package me.xjanua.aitranswatch.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import me.xjanua.aitranswatch.dto.customer.CustomerPageResponse;
import me.xjanua.aitranswatch.dto.customer.CustomerRequest;
import me.xjanua.aitranswatch.dto.customer.CustomerResponse;
import me.xjanua.aitranswatch.enums.CustomerStatus;
import me.xjanua.aitranswatch.model.Customer;
import me.xjanua.aitranswatch.repository.CustomerRepository;

@Service
@Transactional(readOnly = true)
public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerPageResponse findAll(int page, int size, String search, CustomerStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        boolean hasSearch = search != null && !search.isBlank();
        Page<Customer> customers;
        if (hasSearch && status != null) {
            customers = customerRepository.findByStatusAndNameContainingIgnoreCaseOrStatusAndCodeContainingIgnoreCase(
                    status, search.trim(), status, search.trim(), pageable);
        } else if (hasSearch) {
            customers = customerRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(search.trim(), search.trim(), pageable);
        } else if (status != null) {
            customers = customerRepository.findByStatus(status, pageable);
        } else {
            customers = customerRepository.findAll(pageable);
        }
        return new CustomerPageResponse(customers.getContent().stream().map(this::toResponse).toList(),
                customers.getNumber(), customers.getSize(), customers.getTotalElements(), customers.getTotalPages());
    }

    public CustomerResponse findById(UUID id) {
        return toResponse(getCustomer(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        String code = request.code().trim();
        if (customerRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã khách hàng đã tồn tại");
        }
        Customer customer = new Customer(code, request.name().trim(), trimToNull(request.contactName()), trimToNull(request.phone()),
                trimToNull(request.email()), trimToNull(request.address()), request.status() == null ? CustomerStatus.ACTIVE : request.status());
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerRequest request) {
        Customer customer = getCustomer(id);
        String code = request.code().trim();
        if (customerRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã khách hàng đã tồn tại");
        }
        customer.update(code, request.name().trim(), trimToNull(request.contactName()), trimToNull(request.phone()),
                trimToNull(request.email()), trimToNull(request.address()), request.status() == null ? customer.getStatus() : request.status());
        return toResponse(customer);
    }

    @Transactional
    public void deactivate(UUID id) {
        getCustomer(id).deactivate();
    }

    private Customer getCustomer(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getCode(), customer.getName(), customer.getContactName(),
                customer.getPhone(), customer.getEmail(), customer.getAddress(), customer.getStatus(), customer.getCreatedAt(), customer.getUpdatedAt());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
