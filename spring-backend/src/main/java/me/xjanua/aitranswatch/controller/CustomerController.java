package me.xjanua.aitranswatch.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import me.xjanua.aitranswatch.dto.RestResponse;
import me.xjanua.aitranswatch.dto.customer.CustomerPageResponse;
import me.xjanua.aitranswatch.dto.customer.CustomerRequest;
import me.xjanua.aitranswatch.dto.customer.CustomerResponse;
import me.xjanua.aitranswatch.enums.CustomerStatus;
import me.xjanua.aitranswatch.service.CustomerService;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public RestResponse<CustomerPageResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CustomerStatus status) {
        return RestResponse.success(customerService.findAll(page, size, search, status));
    }

    @GetMapping("/{id}")
    public RestResponse<CustomerResponse> findById(@PathVariable UUID id) {
        return RestResponse.success(customerService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DISPATCHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public RestResponse<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return RestResponse.success(customerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DISPATCHER')")
    public RestResponse<CustomerResponse> update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
        return RestResponse.success(customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DISPATCHER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        customerService.deactivate(id);
    }
}
