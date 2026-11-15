CREATE TABLE roles (
    id BINARY(16) NOT NULL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_roles_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users (
    id BINARY(16) NOT NULL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    status VARCHAR(30) NOT NULL,
    last_login_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by BINARY(16), updated_by BINARY(16),
    CONSTRAINT uk_users_username UNIQUE (username), CONSTRAINT uk_users_email UNIQUE (email),
    INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_roles (
    user_id BINARY(16) NOT NULL, role_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE drivers (
    id BINARY(16) NOT NULL PRIMARY KEY, user_id BINARY(16),
    employee_code VARCHAR(50) NOT NULL, license_number VARCHAR(100) NOT NULL,
    license_class VARCHAR(50) NOT NULL, license_expiry_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL, safety_score DECIMAL(5,2) NOT NULL DEFAULT 100.00,
    total_distance_km DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by BINARY(16), updated_by BINARY(16),
    CONSTRAINT uk_drivers_user_id UNIQUE (user_id), CONSTRAINT uk_drivers_employee_code UNIQUE (employee_code), CONSTRAINT uk_drivers_license_number UNIQUE (license_number),
    CONSTRAINT fk_drivers_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_drivers_status (status), INDEX idx_drivers_license_expiry (license_expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vehicles (
    id BINARY(16) NOT NULL PRIMARY KEY, license_plate VARCHAR(30) NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL, brand VARCHAR(100), model VARCHAR(100), manufacture_year INT,
    max_payload_kg DECIMAL(12,2) NOT NULL, current_odometer_km DECIMAL(12,2) NOT NULL DEFAULT 0,
    fuel_level_percent DECIMAL(5,2), status VARCHAR(30) NOT NULL,
    registration_expiry_date DATE, insurance_expiry_date DATE,
    maintenance_interval_km DECIMAL(12,2), next_maintenance_odometer_km DECIMAL(12,2),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by BINARY(16), updated_by BINARY(16), CONSTRAINT uk_vehicles_license_plate UNIQUE (license_plate),
    INDEX idx_vehicles_status (status), INDEX idx_vehicles_registration_expiry (registration_expiry_date), INDEX idx_vehicles_insurance_expiry (insurance_expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE customers (
    id BINARY(16) NOT NULL PRIMARY KEY, code VARCHAR(50) NOT NULL, name VARCHAR(200) NOT NULL,
    contact_name VARCHAR(150), phone VARCHAR(30), email VARCHAR(255), address VARCHAR(500), status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by BINARY(16), updated_by BINARY(16), CONSTRAINT uk_customers_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE transport_orders (
    id BINARY(16) NOT NULL PRIMARY KEY, order_code VARCHAR(50) NOT NULL, customer_id BINARY(16) NOT NULL,
    status VARCHAR(30) NOT NULL, priority VARCHAR(30) NOT NULL, cargo_name VARCHAR(255) NOT NULL,
    weight_kg DECIMAL(12,2) NOT NULL, pickup_address VARCHAR(500) NOT NULL, pickup_latitude DECIMAL(10,7), pickup_longitude DECIMAL(10,7),
    delivery_address VARCHAR(500) NOT NULL, delivery_latitude DECIMAL(10,7), delivery_longitude DECIMAL(10,7), delivery_deadline DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by BINARY(16), updated_by BINARY(16), CONSTRAINT uk_transport_orders_order_code UNIQUE (order_code),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id), INDEX idx_orders_customer_status (customer_id, status), INDEX idx_orders_status_deadline (status, delivery_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trips (
    id BINARY(16) NOT NULL PRIMARY KEY, trip_code VARCHAR(50) NOT NULL, vehicle_id BINARY(16) NOT NULL, driver_id BINARY(16) NOT NULL,
    status VARCHAR(40) NOT NULL, scheduled_start_at DATETIME(6) NOT NULL, actual_start_at DATETIME(6), estimated_arrival_at DATETIME(6), actual_end_at DATETIME(6),
    version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by BINARY(16), updated_by BINARY(16), CONSTRAINT uk_trips_trip_code UNIQUE (trip_code),
    CONSTRAINT fk_trips_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id), CONSTRAINT fk_trips_driver FOREIGN KEY (driver_id) REFERENCES drivers(id),
    INDEX idx_trips_status_scheduled (status, scheduled_start_at), INDEX idx_trips_vehicle_time (vehicle_id, scheduled_start_at), INDEX idx_trips_driver_time (driver_id, scheduled_start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE gps_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, trip_id BINARY(16) NOT NULL, vehicle_id BINARY(16) NOT NULL,
    latitude DECIMAL(10,7) NOT NULL, longitude DECIMAL(10,7) NOT NULL, speed_kmh DECIMAL(6,2) NOT NULL, heading_degree DECIMAL(6,2), engine_on BOOLEAN NOT NULL DEFAULT TRUE, recorded_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_gps_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE, CONSTRAINT fk_gps_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    INDEX idx_gps_trip_recorded_at (trip_id, recorded_at), INDEX idx_gps_vehicle_recorded_at (vehicle_id, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE alerts (
    id BINARY(16) NOT NULL PRIMARY KEY, trip_id BINARY(16), vehicle_id BINARY(16), driver_id BINARY(16), alert_type VARCHAR(50) NOT NULL, severity VARCHAR(20) NOT NULL, status VARCHAR(30) NOT NULL,
    evidence JSON, occurred_at DATETIME(6) NOT NULL, handled_by BINARY(16), handled_at DATETIME(6), handling_note VARCHAR(1000), version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_alerts_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE, CONSTRAINT fk_alerts_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id), CONSTRAINT fk_alerts_driver FOREIGN KEY (driver_id) REFERENCES drivers(id), CONSTRAINT fk_alerts_handled_by FOREIGN KEY (handled_by) REFERENCES users(id),
    INDEX idx_alerts_status_severity_occurred (status, severity, occurred_at), INDEX idx_alerts_trip_occurred (trip_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
