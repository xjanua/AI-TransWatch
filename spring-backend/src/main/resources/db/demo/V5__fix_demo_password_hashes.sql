-- Sửa hash mật khẩu tài khoản demo: hash cũ trong V3 không khớp với mật khẩu "password".
-- Hash dưới đây đã được xác minh bằng BCryptPasswordEncoder (độ dài 60, matches("password") = true).
-- Chỉ áp dụng ở profile dev/demo; production không nạp thư mục db/demo.
UPDATE users
SET password_hash = '$2a$10$K0JS.Yaji4VL9hMPvI1QvuY27ih7XUr9yAJKKuqlWL36UszKAzKVG'
WHERE username IN ('admin', 'dispatcher', 'driver');
