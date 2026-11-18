package me.xjanua.aitranswatch.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import me.xjanua.aitranswatch.enums.CustomerStatus;

public record CustomerRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 150) String contactName,
        @Size(max = 30) String phone,
        @Email @Size(max = 255) String email,
        @Size(max = 500) String address,
        CustomerStatus status) {
}
