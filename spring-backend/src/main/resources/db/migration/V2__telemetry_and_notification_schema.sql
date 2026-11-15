CREATE TABLE maintenance_records (
    id BINARY(16) NOT NULL PRIMARY KEY, vehicle_id BINARY(16) NOT NULL, maintenance_type VARCHAR(30) NOT NULL, status VARCHAR(30) NOT NULL,
    description VARCHAR(1000), odometer_km DECIMAL(12,2), scheduled_date DATE, completed_at DATETIME(6), cost DECIMAL(14,2),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by BINARY(16), updated_by BINARY(16), CONSTRAINT fk_maintenance_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    INDEX idx_maintenance_vehicle_status (vehicle_id, status), INDEX idx_maintenance_scheduled_date (scheduled_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trip_orders (
    trip_id BINARY(16) NOT NULL, transport_order_id BINARY(16) NOT NULL, sequence_no INT NOT NULL, assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (trip_id, transport_order_id), CONSTRAINT uk_trip_orders_sequence UNIQUE (trip_id, sequence_no),
    CONSTRAINT fk_trip_orders_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE, CONSTRAINT fk_trip_orders_order FOREIGN KEY (transport_order_id) REFERENCES transport_orders(id),
    INDEX idx_trip_orders_order (transport_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trip_route_points (
    id BINARY(16) NOT NULL PRIMARY KEY, trip_id BINARY(16) NOT NULL, sequence_no INT NOT NULL, point_type VARCHAR(30) NOT NULL,
    latitude DECIMAL(10,7) NOT NULL, longitude DECIMAL(10,7) NOT NULL, allowed_radius_m DECIMAL(10,2), planned_arrival_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_trip_route_points_sequence UNIQUE (trip_id, sequence_no), CONSTRAINT fk_route_points_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    INDEX idx_route_points_trip_sequence (trip_id, sequence_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_predictions (
    id BINARY(16) NOT NULL PRIMARY KEY, trip_id BINARY(16) NOT NULL, model_type VARCHAR(30) NOT NULL, model_version VARCHAR(100) NOT NULL,
    eta_minutes DECIMAL(10,2), late_probability DECIMAL(5,4), risk_level VARCHAR(20), anomaly_score DECIMAL(10,6), feature_snapshot JSON, explanation JSON, predicted_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_predictions_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    INDEX idx_ai_predictions_trip_predicted (trip_id, predicted_at), INDEX idx_ai_predictions_type_predicted (model_type, predicted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notifications (
    id BINARY(16) NOT NULL PRIMARY KEY, user_id BINARY(16) NOT NULL, alert_id BINARY(16), type VARCHAR(30) NOT NULL, title VARCHAR(255) NOT NULL, content VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL, read_at DATETIME(6), created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id), CONSTRAINT fk_notifications_alert FOREIGN KEY (alert_id) REFERENCES alerts(id) ON DELETE SET NULL,
    INDEX idx_notifications_user_status_created (user_id, status, created_at), INDEX idx_notifications_alert (alert_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
