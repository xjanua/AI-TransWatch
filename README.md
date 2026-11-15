# AI-TransWatch

Ứng dụng web quản lý vận tải có giám sát GPS, cảnh báo vận hành theo thời gian thực và mô hình AI dự báo ETA/rủi ro trễ, phát hiện bất thường hành trình.

## Tính năng chính

- Quản lý xe, tài xế, khách hàng, đơn vận chuyển và điều phối chuyến có kiểm tra ràng buộc.
- Bản đồ giám sát đội xe (Leaflet/OpenStreetMap) lấy vị trí GPS mới nhất.
- Rule engine cảnh báo: vượt tốc độ, lệch tuyến, dừng bất thường, lái liên tục, mất tín hiệu GPS, giấy tờ sắp hết hạn, đến hạn bảo dưỡng.
- Cảnh báo đẩy realtime lên dashboard qua WebSocket/STOMP, kèm trung tâm xử lý cảnh báo.
- AI service dự báo ETA, xác suất giao trễ và phát hiện bất thường (Random Forest + Isolation Forest).
- Dashboard KPI, biểu đồ và báo cáo thống kê.

## Cấu trúc

- `frontend/`: React + TypeScript + Vite.
- `spring-backend/`: Java 21 + Spring Boot 4 + MySQL + JPA/Flyway.
- `ai-service/`: FastAPI + scikit-learn.
- `docs/`: kế hoạch, ERD và tài liệu kỹ thuật.

## Khởi chạy môi trường phát triển

1. Sao chép `.env.example` thành `.env` và điền thông tin MySQL cục bộ. Không commit `.env`.
2. Tạo schema MySQL `ai_transwatch` nếu tài khoản không có quyền tạo schema.
3. Chạy backend: `cd spring-backend; .\gradlew.bat bootRun`.
4. Chạy AI service: `cd ai-service; python -m venv .venv; .\.venv\Scripts\pip install -r requirements.txt; .\.venv\Scripts\uvicorn app.main:app --reload --port 8000`.
5. Chạy frontend: `cd frontend; npm ci; npm run dev`.

## Chạy bằng Docker Compose

```powershell
docker compose up --build
# Giao diện: http://localhost:8081
```

> Cấu hình Docker đã có sẵn nhưng **chưa được chạy thử** vì máy phát triển không cài Docker;
> xem ghi chú trong [Hướng dẫn chạy](docs/HUONG_DAN_CHAY.md).

## Kiểm thử nhanh

```powershell
# Backend
cd spring-backend; .\gradlew.bat clean test

# AI service
cd ..\ai-service; .\.venv\Scripts\python.exe -m pytest -q

# Frontend
cd ..\frontend; npm run build

# Kiểm thử chấp nhận end-to-end (cần backend + AI service + MySQL đang chạy)
cd ..; .\scripts\verify-e2e.ps1

# Kênh cảnh báo realtime (WebSocket/STOMP)
node .\scripts\verify-realtime.mjs
```

## Tài khoản demo (chỉ phát triển/bảo vệ)

Profile `dev` nạp migration `db/demo/V3` để tạo ba tài khoản với mật khẩu chung **`password`**. Không dùng các migration demo trên môi trường production; phải thay đổi hoặc xóa các tài khoản này trước khi triển khai thật.

| Vai trò | Username | Mật khẩu |
| --- | --- | --- |
| Admin | `admin` | `password` |
| Điều phối viên | `dispatcher` | `password` |
| Tài xế | `driver` | `password` |

`POST /api/auth/login` nhận `{ "username": "admin", "password": "password" }`; dùng `accessToken` trả về trong header `Authorization: Bearer <token>` để gọi `GET /api/auth/me` và các endpoint được bảo vệ.

## Tài liệu

- [Kế hoạch dự án](docs/KE_HOACH_DU_AN.md)
- [ERD](docs/ERD.md)
- [Hướng dẫn chạy đầy đủ](docs/HUONG_DAN_CHAY.md)
- [Kịch bản bảo vệ](docs/KICH_BAN_BAO_VE.md)
- [Kết quả kiểm thử](docs/KET_QUA_KIEM_THU.md)
