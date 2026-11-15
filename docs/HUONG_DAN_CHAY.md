# Hướng dẫn chạy AI-TransWatch trên Windows

Tài liệu này hướng dẫn khởi chạy ba thành phần của hệ thống:

- `frontend/`: React + TypeScript + Vite (giao diện web).
- `spring-backend/`: Spring Boot 4 + Java 21 (REST API, phân quyền, nghiệp vụ và Flyway).
- `ai-service/`: FastAPI + scikit-learn (dự báo ETA, rủi ro trễ, bất thường).

Xem thêm [Kế hoạch dự án](KE_HOACH_DU_AN.md) và [ERD](ERD.md).

## 1. Điều kiện tiên quyết

Cài đặt và kiểm tra các công cụ sau trong PowerShell:

| Công cụ | Phiên bản khuyến nghị | Lệnh kiểm tra |
| --- | --- | --- |
| JDK | Java 21 LTS | `java -version` |
| Node.js / npm | Node.js 20+ | `node --version`, `npm --version` |
| Python | Python 3.12+ | `python --version` |
| MySQL Server | MySQL 8+ | `mysql --version` *(nếu đã cài client)* |

Backend dùng Gradle Wrapper nên **không cần** cài Gradle toàn cục. Các lệnh dưới đây chạy từ thư mục gốc dự án `D:\Programing\DACN`.

> **Lưu ý MySQL remote:** máy chủ mục tiêu là `db.xjanua.tech:3306` và đã được xác minh kết nối, chạy đủ migration trên schema `ai_transwatch`. Nếu môi trường khác gặp timeout/refused (firewall, allowlist IP), hãy dùng MySQL local hoặc VPN/tunnel. Không cố chạy Flyway vào database production chưa sao lưu.

## 2. Cấu hình biến môi trường an toàn

1. Sao chép file mẫu; file `.env` đã nằm trong `.gitignore` và không được commit:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Mở `.env` và thay **toàn bộ placeholder** bằng dữ liệu cục bộ/an toàn. Ví dụ khi dùng MySQL local:

   ```dotenv
   MYSQL_HOST=localhost
   MYSQL_PORT=3306
   MYSQL_DATABASE=ai_transwatch
   MYSQL_USERNAME=<tai_khoan_mysql>
   MYSQL_PASSWORD=<mat_khau_mysql>

   SPRING_PROFILES_ACTIVE=dev
   SERVER_PORT=8080
   JWT_BASE64_SECRET=<chuoi_base64_bi_mat_toi_thieu_32_byte>
   FRONTEND_ALLOWED_ORIGINS=http://localhost:5173
   AI_SERVICE_BASE_URL=http://localhost:8000
   AI_SERVICE_PORT=8000
   ```

3. Không gửi `.env`, mật khẩu MySQL hoặc `JWT_BASE64_SECRET` qua Git, ảnh chụp màn hình hay báo cáo. Có thể tạo secret base64 ở PowerShell:

   ```powershell
   $bytes = New-Object byte[] 48
   [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
   [Convert]::ToBase64String($bytes)
   ```

4. Nếu tài khoản MySQL không có quyền tạo database, tạo sẵn schema (không chèn mật khẩu vào lệnh):

   ```sql
   CREATE DATABASE ai_transwatch CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

Khi Spring Boot chạy lần đầu với profile `dev`, Flyway tự áp dụng các migration schema và seed tài khoản demo. **Không chạy các migration seed V3/V4 vào database production**: chúng chứa tài khoản mật khẩu demo và dữ liệu mô phỏng, chỉ phù hợp môi trường `dev/demo` của đồ án. Cấu hình JPA là `validate`, do đó schema phải được Flyway tạo đúng thay vì Hibernate tự thay đổi database.

## 3. Chuẩn bị và train mô hình AI

Mở PowerShell thứ nhất:

```powershell
Set-Location D:\Programing\DACN\ai-service
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -c "from app.ml_workflow import train_and_save_models; train_and_save_models(); print('Da train model AI')"
.\.venv\Scripts\python.exe -m pytest -q
```

Lệnh train sinh dữ liệu telemetry tổng hợp tái lập (seed cố định), sau đó lưu artifact `telemetry_models.joblib` dưới `app/models/`. Artifact là dữ liệu sinh tự động, không nên commit. Nếu chưa train, `/v1/predictions/analyze` vẫn hoạt động với baseline minh bạch.

Khởi chạy FastAPI:

```powershell
Set-Location D:\Programing\DACN\ai-service
.\.venv\Scripts\uvicorn.exe app.main:app --reload --host 127.0.0.1 --port 8000
```

Kiểm tra:

- [AI health](http://127.0.0.1:8000/health)
- [AI Swagger](http://127.0.0.1:8000/docs)

## 4. Khởi chạy Spring Boot backend

Mở PowerShell thứ hai. Nạp `.env` vào process PowerShell rồi chạy backend:

```powershell
Set-Location D:\Programing\DACN
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
    [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process')
  }
}
Set-Location .\spring-backend
.\gradlew.bat clean test
.\gradlew.bat bootRun
```

Kiểm tra sau khi khởi động:

- [Backend health](http://127.0.0.1:8080/api/health)
- [Spring Boot Actuator health](http://127.0.0.1:8080/actuator/health)
- [Backend Swagger UI](http://127.0.0.1:8080/swagger-ui.html)
- [OpenAPI JSON](http://127.0.0.1:8080/api-docs)

Nếu backend lỗi kết nối MySQL, xác minh lần lượt `MYSQL_HOST`, port, firewall, quyền user/schema và tình trạng dịch vụ MySQL. Với remote `db.xjanua.tech:3306`, cần xử lý vấn đề kết nối mạng đã nêu ở mục 1 trước.

## 5. Khởi chạy frontend

Mở PowerShell thứ ba:

```powershell
Set-Location D:\Programing\DACN\frontend
npm ci
$env:VITE_API_BASE_URL = 'http://localhost:8080/api'
npm run build
npm run dev
```

Mở URL Vite in ra trên terminal (mặc định [http://localhost:5173](http://localhost:5173)). Nếu frontend được chạy ở host/port khác, đặt `FRONTEND_ALLOWED_ORIGINS` trong `.env` tương ứng rồi restart backend.

## 6. Tài khoản demo và đăng nhập

Flyway migration `V3` seed các tài khoản sau **chỉ để phát triển/bảo vệ**. Mật khẩu chung là `password`; phải đổi hoặc xóa trước khi triển khai thật.

| Vai trò | Username | Mật khẩu | Mục đích demo |
| --- | --- | --- | --- |
| Admin | `admin` | `password` | Quản trị toàn hệ thống |
| Điều phối viên | `dispatcher` | `password` | Tạo đơn/chuyến, xử lý alert |
| Tài xế | `driver` | `password` | Gửi GPS cho chuyến được phân công |

Đăng nhập ở `/login`. API tương ứng:

```http
POST /api/auth/login
Content-Type: application/json

{"username":"dispatcher","password":"password"}
```

Dùng `accessToken` trả về làm `Authorization: Bearer <token>` khi gọi API được bảo vệ. Kiểm tra phiên với `GET /api/auth/me`.

## 7. Danh sách API chính

Các endpoint đầy đủ được xem và thử trực tiếp trong [Swagger UI](http://127.0.0.1:8080/swagger-ui.html).

| Nhóm | Endpoint tiêu biểu |
| --- | --- |
| Xác thực | `POST /api/auth/login`, `GET /api/auth/me` |
| Dữ liệu nền | `/api/vehicles`, `/api/drivers`, `/api/customers` |
| Đơn vận chuyển | `/api/orders` |
| Điều phối | `GET/POST /api/trips`, `PATCH /api/trips/{id}/status` |
| GPS | `POST /api/trips/{tripId}/gps`, `GET /api/trips/{tripId}/gps/latest` |
| Cảnh báo | `GET /api/alerts`, `PATCH /api/alerts/{id}/status` |
| Giám sát trực tuyến | `GET /api/monitoring/live` |
| Báo cáo | `GET /api/reports/overview` |
| AI theo chuyến | `POST /api/trips/{tripId}/analysis` |
| Mô phỏng GPS (dev/demo) | `POST /api/demo/trips/{tripId}/simulate-step` |
| Chạy kiểm tra cảnh báo định kỳ (dev/demo) | `POST /api/demo/trips/alerts/run-checks` |
| Cảnh báo realtime | WebSocket/STOMP: `ws://localhost:8080/ws?token=<JWT>`, kênh `/topic/alerts` |

`POST /api/trips/{tripId}/analysis` dùng GPS mới nhất và gọi FastAPI `/v1/predictions/analyze`. Thiếu GPS trả `409`; AI service không chạy/sai `AI_SERVICE_BASE_URL` trả `503` với thông báo cấu hình rõ ràng.

`GET /api/monitoring/live` cung cấp vị trí mới nhất của các chuyến đang vận hành để dashboard vẽ bản đồ Leaflet/OpenStreetMap; giao diện tự động làm mới mỗi 15 giây.

### 7.1. Rule engine cảnh báo và ngưỡng cấu hình

| Luật | Ngưỡng mặc định | Thuộc tính cấu hình |
| --- | --- | --- |
| Vượt tốc độ | > 80 km/h | `alerts.speed-limit-kmh` |
| Lệch tuyến | > 300 m so với lộ trình dự kiến | `alerts.route-deviation-meters` |
| Dừng bất thường | dừng ≥ 15 phút khi động cơ bật | `alerts.abnormal-stop-minutes` |
| Lái liên tục | > 240 phút không nghỉ đủ | `alerts.continuous-driving-minutes` |
| Mất tín hiệu GPS | không có telemetry trong 10 phút | `alerts.gps-signal-timeout-minutes` |
| Giấy tờ sắp hết hạn | còn ≤ 30 ngày | `alerts.document-expiry-warning-days` |
| Đến hạn bảo dưỡng | odometer ≥ mốc bảo dưỡng kế tiếp | – (dữ liệu xe) |
| Chống trùng cảnh báo | 6 giờ cho cùng loại + đối tượng đang mở | `alerts.duplicate-suppression-hours` |

Các kiểm tra định kỳ (mất GPS, giấy tờ, bảo dưỡng) chạy mỗi 60 giây
(`alerts.check-interval-ms`). Khi demo, có thể chạy ngay bằng
`POST /api/demo/trips/alerts/run-checks` (chỉ ADMIN, chỉ tồn tại ở profile `dev`/`demo`).

### 7.2. Cảnh báo realtime qua WebSocket/STOMP

Backend mở endpoint STOMP tại `/ws`, xác thực JWT ngay ở bước handshake (token truyền qua tham số
`?token=`), và phát cảnh báo mới lên kênh `/topic/alerts` sau khi giao dịch commit.

- Frontend tự kết nối sau khi đăng nhập; địa chỉ mặc định `ws://localhost:8080/ws`, đổi bằng biến
  `VITE_WS_BASE_URL` khi chạy `npm run dev`.
- Dashboard hiển thị trạng thái kết nối trên thanh tiêu đề và vẫn làm mới định kỳ 15 giây như
  phương án dự phòng.
- Kiểm thử kênh realtime bằng script có sẵn:

  ```powershell
  Set-Location D:\Programing\DACN
  node .\scripts\verify-realtime.mjs
  ```

## 8. Lệnh kiểm thử nhanh

```powershell
# Backend
Set-Location D:\Programing\DACN\spring-backend
.\gradlew.bat clean test

# AI service
Set-Location D:\Programing\DACN\ai-service
.\.venv\Scripts\python.exe -m pytest -q

# Frontend
Set-Location D:\Programing\DACN\frontend
npm run build
```

## 8.1. Kiểm thử chấp nhận end-to-end

Khi MySQL, backend (`8080`) và AI service (`8000`) đang chạy, thực hiện kiểm thử toàn luồng
nghiệp vụ bằng script có sẵn:

```powershell
Set-Location D:\Programing\DACN
.\scripts\verify-e2e.ps1
```

Script kiểm tra: sức khỏe dịch vụ, đăng nhập và phân quyền (`401`/`403`), dữ liệu seed, kiểm tra
dữ liệu đầu vào, tạo xe/tài xế/đơn/chuyến, chống trùng điều phối (`409`), chuyển trạng thái chuyến,
ghi GPS, sinh cảnh báo, xử lý cảnh báo, phân tích AI, các luật cảnh báo mở rộng, cảnh báo định kỳ,
kênh realtime và giải phóng tài nguyên sau khi hoàn tất chuyến.
Kết quả mong đợi: `=== Result: 45 PASS, 0 FAIL ===`.

Script chỉ tạo dữ liệu mới với tiền tố `E2E-` và không sửa dữ liệu seed demo.

## 9. Chạy toàn hệ thống bằng Docker Compose (chưa kiểm chứng tại máy hiện tại)

Repo có sẵn `Dockerfile` cho cả ba dịch vụ, `nginx.conf` chuyển tiếp `/api` và nâng cấp WebSocket
cho `/ws`, cùng `docker-compose.yml` gồm MySQL, AI service, backend và frontend:

```powershell
Set-Location D:\Programing\DACN
docker compose up --build
```

| Dịch vụ | Địa chỉ |
| --- | --- |
| Giao diện (nginx) | http://localhost:8081 |
| API qua nginx | http://localhost:8081/api/health |
| Backend trực tiếp | http://localhost:8080/api/health |
| AI service | http://localhost:8000/health |
| MySQL | `localhost:3307` (ánh xạ từ 3306 trong container) |

> **Cảnh báo trung thực:** máy phát triển hiện tại **không cài Docker**, nên cấu hình này mới chỉ được
> kiểm tra cú pháp YAML và nội dung Dockerfile, chưa chạy thực tế. Trước khi dùng cho báo cáo, hãy chạy
> thử và đổi `MYSQL_ROOT_PASSWORD`, `JWT_BASE64_SECRET` khỏi giá trị mặc định.

## 10. Dừng dịch vụ

Tại mỗi cửa sổ terminal đang chạy dịch vụ, nhấn `Ctrl+C`. Không xóa schema/volume/dữ liệu demo trong lúc đang bảo vệ nếu chưa sao lưu.

## 11. Giới hạn hiện tại cần nêu trung thực

- Dữ liệu ML ban đầu là dữ liệu tổng hợp dùng cho học thuật/demo; cần train lại bằng telemetry thực tế trước khi dùng quyết định vận hành thật.
- GPS đang nhận qua REST API và endpoint mô phỏng; tích hợp thiết bị IoT thật, bản đồ/định tuyến thật là hướng mở rộng.
- Cấu hình `docker compose` chưa được chạy thử trong môi trường phát triển hiện tại vì máy không có Docker.
- Các tài khoản demo và CORS localhost không phù hợp môi trường production.
- Nhật ký kiểm thử thực tế và danh sách lỗi tích hợp đã khắc phục được ghi tại [Kết quả kiểm thử](KET_QUA_KIEM_THU.md).

## 12. Xử lý sự cố thường gặp

| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
| --- | --- | --- |
| `Schema validation: missing table [...]` | Flyway chưa chạy trước Hibernate | Kiểm tra dependency `spring-boot-starter-flyway` còn trong `build.gradle` và `spring.flyway.locations` đúng |
| Đăng nhập `admin/password` trả `401` | Hash mật khẩu seed không khớp | Đảm bảo migration `db/demo/V5__fix_demo_password_hashes.sql` đã được áp dụng (profile `dev`) |
| `POST /api/trips/{id}/analysis` trả `503` | FastAPI chưa chạy hoặc sai `AI_SERVICE_BASE_URL` | Khởi động uvicorn cổng 8000, kiểm tra `/health`; thông báo lỗi có kèm chi tiết phản hồi của FastAPI |
| Trình duyệt báo lỗi CORS | Origin frontend không nằm trong `FRONTEND_ALLOWED_ORIGINS` | Cập nhật biến trong `.env` rồi khởi động lại backend |
| Cổng 8080/8000 đang bị chiếm | Tiến trình cũ chưa dừng | Dừng tiến trình cũ trước khi chạy lại (`Ctrl+C` ở cửa sổ tương ứng) |
