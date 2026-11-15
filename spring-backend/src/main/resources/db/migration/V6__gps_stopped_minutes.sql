-- Lưu thời gian dừng (phút) kèm mỗi bản ghi GPS để phát hiện lái liên tục và dừng bất thường
-- mà không cần suy đoán lại từ chuỗi tốc độ.
ALTER TABLE gps_logs
    ADD COLUMN stopped_minutes INT NULL AFTER engine_on;
