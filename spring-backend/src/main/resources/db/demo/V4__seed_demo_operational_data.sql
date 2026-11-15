-- Dữ liệu demo vận hành cho môi trường phát triển/bảo vệ.
-- UUID BINARY(16) cố định, liên kết tài khoản driver đã seed tại V3.

INSERT INTO vehicles (
    id, license_plate, vehicle_type, brand, model, manufacture_year,
    max_payload_kg, current_odometer_km, fuel_level_percent, status,
    registration_expiry_date, insurance_expiry_date, maintenance_interval_km, next_maintenance_odometer_km
) VALUES
    (UNHEX('20000000000000000000000000000001'), '51D-882.19', 'TRUCK', 'Hyundai', 'Mighty EX8', 2023,
     5000.00, 18420.50, 74.00, 'ON_TRIP', '2027-12-31', '2027-06-30', 5000.00, 20000.00),
    (UNHEX('20000000000000000000000000000002'), '60C-235.68', 'TRUCK', 'Isuzu', 'QKR 270', 2022,
     3000.00, 24680.00, 88.00, 'AVAILABLE', '2027-10-15', '2027-04-15', 5000.00, 25000.00);

INSERT INTO drivers (
    id, user_id, employee_code, license_number, license_class, license_expiry_date,
    status, safety_score, total_distance_km
) VALUES
    (UNHEX('30000000000000000000000000000001'), UNHEX('10000000000000000000000000000003'),
     'DRV-DEMO-001', '79FC123456', 'C', '2029-12-31', 'ON_TRIP', 96.50, 42580.00);

INSERT INTO customers (id, code, name, contact_name, phone, email, address, status) VALUES
    (UNHEX('40000000000000000000000000000001'), 'CUS-DEMO-001', 'Công ty TNHH Thương mại An Phát', 'Nguyễn Minh Anh', '0909123456', 'anh@anphat.example', 'Quận 1, TP. Hồ Chí Minh', 'ACTIVE'),
    (UNHEX('40000000000000000000000000000002'), 'CUS-DEMO-002', 'Công ty Logistics Bình Dương', 'Trần Quốc Bình', '0918123456', 'binh@binhduong-logistics.example', 'Thuận An, Bình Dương', 'ACTIVE');

INSERT INTO transport_orders (
    id, order_code, customer_id, status, priority, cargo_name, weight_kg,
    pickup_address, pickup_latitude, pickup_longitude, delivery_address, delivery_latitude, delivery_longitude, delivery_deadline
) VALUES
    (UNHEX('50000000000000000000000000000001'), 'ORD-DEMO-001', UNHEX('40000000000000000000000000000001'), 'ASSIGNED', 'HIGH', 'Linh kiện điện tử', 1850.00,
     'Khu công nghệ cao, TP. Thủ Đức', 10.8427000, 106.8099000, 'KCN Sóng Thần, Dĩ An, Bình Dương', 10.9065000, 106.7699000, '2030-01-15 16:00:00.000000'),
    (UNHEX('50000000000000000000000000000002'), 'ORD-DEMO-002', UNHEX('40000000000000000000000000000002'), 'PENDING', 'NORMAL', 'Thiết bị văn phòng', 950.00,
     'Quận 7, TP. Hồ Chí Minh', 10.7298000, 106.7219000, 'Biên Hòa, Đồng Nai', 10.9574000, 106.8426000, '2030-01-16 17:00:00.000000');

INSERT INTO trips (
    id, trip_code, vehicle_id, driver_id, status, scheduled_start_at, estimated_arrival_at
) VALUES
    (UNHEX('60000000000000000000000000000001'), 'TRIP-DEMO-001', UNHEX('20000000000000000000000000000001'), UNHEX('30000000000000000000000000000001'),
     'IN_TRANSIT', '2030-01-15 13:00:00.000000', '2030-01-15 15:30:00.000000');

INSERT INTO trip_orders (trip_id, transport_order_id, sequence_no, assigned_at) VALUES
    (UNHEX('60000000000000000000000000000001'), UNHEX('50000000000000000000000000000001'), 1, '2030-01-15 12:30:00.000000');

INSERT INTO trip_route_points (id, trip_id, sequence_no, point_type, latitude, longitude, allowed_radius_m, planned_arrival_at) VALUES
    (UNHEX('70000000000000000000000000000001'), UNHEX('60000000000000000000000000000001'), 1, 'ORIGIN', 10.8427000, 106.8099000, 100.00, '2030-01-15 13:00:00.000000'),
    (UNHEX('70000000000000000000000000000002'), UNHEX('60000000000000000000000000000001'), 2, 'DESTINATION', 10.9065000, 106.7699000, 100.00, '2030-01-15 15:30:00.000000');
