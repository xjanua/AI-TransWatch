-- Tài khoản demo chỉ dùng cho môi trường phát triển/bảo vệ đồ án.
-- BCrypt hash dưới đây là mật khẩu chung: password
INSERT INTO roles (id, code, name, description) VALUES
    (UNHEX('00000000000000000000000000000001'), 'ADMIN', 'Quản trị viên', 'Quản trị toàn bộ hệ thống'),
    (UNHEX('00000000000000000000000000000002'), 'DISPATCHER', 'Điều phối viên', 'Điều phối chuyến và xử lý cảnh báo'),
    (UNHEX('00000000000000000000000000000003'), 'DRIVER', 'Tài xế', 'Xem và cập nhật chuyến được giao');

INSERT INTO users (id, username, email, password_hash, full_name, phone, status) VALUES
    (UNHEX('10000000000000000000000000000001'), 'admin', 'admin@ai-transwatch.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Quản trị viên Demo', '0900000001', 'ACTIVE'),
    (UNHEX('10000000000000000000000000000002'), 'dispatcher', 'dispatcher@ai-transwatch.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Điều phối viên Demo', '0900000002', 'ACTIVE'),
    (UNHEX('10000000000000000000000000000003'), 'driver', 'driver@ai-transwatch.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Tài xế Demo', '0900000003', 'ACTIVE');

INSERT INTO user_roles (user_id, role_id) VALUES
    (UNHEX('10000000000000000000000000000001'), UNHEX('00000000000000000000000000000001')),
    (UNHEX('10000000000000000000000000000002'), UNHEX('00000000000000000000000000000002')),
    (UNHEX('10000000000000000000000000000003'), UNHEX('00000000000000000000000000000003'));
