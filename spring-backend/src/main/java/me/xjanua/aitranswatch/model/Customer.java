package me.xjanua.aitranswatch.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.xjanua.aitranswatch.enums.CustomerStatus;

@Entity
@Table(name = "customers")
@Getter
@NoArgsConstructor
public class Customer {
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CustomerStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Customer(String code, String name, String contactName, String phone, String email, String address, CustomerStatus status) {
        this.code = code;
        this.name = name;
        this.contactName = contactName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.status = status;
    }

    public void update(String code, String name, String contactName, String phone, String email, String address, CustomerStatus status) {
        this.code = code;
        this.name = name;
        this.contactName = contactName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.status = status;
    }

    public void deactivate() {
        this.status = CustomerStatus.INACTIVE;
    }
}
