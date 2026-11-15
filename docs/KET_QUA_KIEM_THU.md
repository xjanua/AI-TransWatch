# Kết quả kiểm thử — AI-TransWatch

Tài liệu ghi lại bằng chứng kiểm thử thực tế của hệ thống, phục vụ phần "Kiểm thử" trong báo cáo đồ án.

## 1. Môi trường kiểm thử

| Thành phần | Phiên bản / địa chỉ |
| --- | --- |
| JDK | Java 21.0.12 LTS |
| Spring Boot | 4.1.0 (Spring Framework 7, Hibernate 7.4.1) |
| Cơ sở dữ liệu | MySQL 8.0.46 — `db.xjanua.tech:3306`, schema `ai_transwatch` |
| Node.js / Vite | Node 24.19.0 — Vite 8.3.8 |
| Python / FastAPI | Python 3.12.10 — FastAPI 0.128.0, scikit-learn 1.7.2 |
| Migration | Flyway 12.4.0 — V1…V5 |

## 2. Kiểm thử chấp nhận end-to-end

Script tái lập: [`scripts/verify-e2e.ps1`](../scripts/verify-e2e.ps1)

```powershell
.\scripts\verify-e2e.ps1
```

Kết quả lần chạy gần nhất trên MySQL thật:

```text
=== Result: 45 PASS, 0 FAIL ===
```

Nội dung đã kiểm chứng:

| Nhóm | Hạng mục kiểm thử |
| --- | --- |
| Sức khỏe | Backend `/api/health` = UP, FastAPI `/health` = UP |
| Xác thực | Đăng nhập admin nhận JWT, `GET /auth/me` trả vai trò `ADMIN` |
| Phân quyền | Sai mật khẩu → `401`, thiếu token → `401`, điều phối viên gọi endpoint chỉ dành cho admin → `403` |
| Dữ liệu nền | Đọc xe, tài xế, khách hàng, chuyến seed `TRIP-DEMO-001` từ MySQL |
| Kiểm tra dữ liệu | Khối lượng đơn âm → `400` |
| Điều phối | Tạo xe, tài xế, đơn, chuyến; xe → `ON_TRIP`; đơn → `ASSIGNED`; điều phối lại xe đang bận → `409` |
| Trạng thái chuyến | `PICKUP_IN_PROGRESS` → `IN_TRANSIT` → `COMPLETED` |
| GPS | Ghi telemetry 95 km/h, đọc lại vị trí mới nhất khớp dữ liệu |
| Cảnh báo | Sinh `SPEEDING/HIGH`, evidence JSON có `speedKmh`, chuyển trạng thái `IN_PROGRESS` → `RESOLVED` |
| AI | ETA dương, `lateProbability ∈ [0,1]`, `riskLevel` hợp lệ, có giải thích tiếng Việt, dùng model đã train (`usingBaseline = false`) |
| Báo cáo | `GET /api/reports/overview` trả tổng hợp, chuỗi 7 ngày đủ 7 điểm và các tỷ lệ trong `[0,1]` |
| Rule engine mở rộng | `ROUTE_DEVIATION`, `CONTINUOUS_DRIVING`, `GPS_SIGNAL_LOST`, `DOCUMENT_EXPIRING`, `MAINTENANCE_DUE` đều phát sinh đúng loại và mức độ |
| Chống trùng cảnh báo | Lần chạy kiểm tra thứ hai không tạo thêm cảnh báo nào |
| Realtime | Client STOMP nhận được cảnh báo do rule engine đẩy ngay sau khi phát sinh |
| Giải phóng tài nguyên | Đơn → `DELIVERED`, xe và tài xế trở lại `AVAILABLE` |

Ví dụ kết quả AI thật thu được:

```json
{
  "etaMinutes": 38.45,
  "lateProbability": 0.52,
  "riskLevel": "MEDIUM",
  "anomalyScore": 1.0,
  "modelVersion": "synthetic-rf-iforest-1.0",
  "usingBaseline": false
}
```

### 2.1. Giám sát trực tuyến cho bản đồ

Endpoint `GET /api/monitoring/live` trả vị trí GPS mới nhất của mọi chuyến chưa kết thúc.
Kết quả thật lấy từ MySQL:

```json
{
  "tripCode": "TRIP-DEMO-001",
  "status": "IN_TRANSIT",
  "vehiclePlate": "51D-882.19",
  "driverName": "Tài xế Demo",
  "latitude": 10.8790000,
  "longitude": 106.7900000,
  "speedKmh": 0.00,
  "recordedAt": "2026-11-15T18:11:39"
}
```

Dashboard dùng dữ liệu này để vẽ marker Leaflet/OpenStreetMap và tự động làm mới mỗi 15 giây.

### 2.2. Kiểm tra CORS cho trình duyệt

Yêu cầu preflight từ origin frontend phải thành công, nếu không trình duyệt sẽ chặn API:

```text
OPTIONS /api/trips  (Origin: http://localhost:5173)
HTTP/1.1 200
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
```

### 2.3. Số liệu báo cáo thật từ MySQL

`GET /api/reports/overview` dùng truy vấn gộp ở tầng repository. Ví dụ kết quả:

```json
{
  "tripsByStatus": { "COMPLETED": 3, "IN_TRANSIT": 1 },
  "ordersByStatus": { "ASSIGNED": 1, "DELIVERED": 3, "PENDING": 1 },
  "vehiclesByStatus": { "AVAILABLE": 5, "ON_TRIP": 1 },
  "alertsBySeverity": { "HIGH": 4, "MEDIUM": 1 },
  "alertsByType": { "SPEEDING": 4, "ABNORMAL_STOP": 1 },
  "onTimeRate": 1.0,
  "completedTripsWithEta": 3,
  "alertHandlingRate": 0.6,
  "totalAlerts": 5,
  "totalTrips": 4,
  "totalOrders": 5
}
```

Tab **Báo cáo** trên dashboard hiển thị biểu đồ cột chuyến xe/cảnh báo theo ngày (7 ngày gần nhất),
biểu đồ tròn cảnh báo theo mức độ, phân bổ trạng thái theo nhóm dữ liệu và các chỉ số tỷ lệ.
Thư viện biểu đồ được tách khỏi bundle chính (lazy load) nên chỉ tải khi mở tab này.

> Script tạo dữ liệu kiểm thử với tiền tố `E2E-` và không sửa dữ liệu seed demo; chạy lại nhiều lần vẫn an toàn.

## 3. Kiểm thử từng dịch vụ

| Dịch vụ | Lệnh | Kết quả |
| --- | --- | --- |
| Backend | `cd spring-backend; .\gradlew.bat test` | `BUILD SUCCESSFUL` — 2 lớp test, 12 test, 0 lỗi |
| AI service | `cd ai-service; .\.venv\Scripts\python.exe -m pytest -q` | `5 passed` |
| Frontend | `cd frontend; npm run build` | Vite build thành công (2470 module; bundle chính 433 kB / gzip 135 kB, chunk báo cáo tải theo yêu cầu 405 kB / gzip 115 kB) |

### 3.1. Kiểm thử đơn vị backend

`TripServiceTransitionTest` kiểm tra luật chuyển trạng thái chuyến bằng Mockito, không cần cơ sở dữ liệu:

| Test | Nội dung |
| --- | --- |
| `allowsScheduledToPickup` | `SCHEDULED → PICKUP_IN_PROGRESS` hợp lệ, ghi nhận thời điểm bắt đầu |
| `rejectsBackwardTransition` | `IN_TRANSIT → SCHEDULED` bị từ chối với `409` |
| `rejectsSkippingTransit` | `PICKUP_IN_PROGRESS → COMPLETED` bị từ chối (phải qua `IN_TRANSIT`) |
| `terminalStatusIsFinal` | Chuyến đã `COMPLETED` không thể chuyển tiếp |
| `completesAndReleasesResources` | Hoàn tất chuyến giải phóng xe và tài xế về `AVAILABLE` |
| `incidentCanResume` | Chuyến ở `INCIDENT` có thể quay lại `IN_TRANSIT` |

`GeoUtilsTest` kiểm tra phần tính khoảng cách dùng cho luật lệch tuyến:

| Test | Nội dung |
| --- | --- |
| `samePointHasZeroDistance` | Hai điểm trùng nhau cho khoảng cách 0 |
| `latitudeDeltaIsAboutOneKilometer` | Chênh 0,01 độ vĩ tuyến ≈ 1,1 km |
| `pointOnSegmentHasNearZeroDistance` | Điểm nằm trên lộ trình có khoảng cách < 50 m |
| `deviatedPointIsFarFromRoute` | Điểm lệch 0,03 độ cách lộ trình > 3 km |
| `emptyRouteReturnsNull` | Lộ trình rỗng trả `null` để bỏ qua luật |
| `projectsOntoSegment` | Khoảng cách tính theo hình chiếu vuông góc lên đoạn |

### 3.2. Rule engine cảnh báo đã kiểm chứng trên dữ liệu thật

| Loại cảnh báo | Điều kiện | Mức độ | Cách kiểm chứng |
| --- | --- | --- | --- |
| `SPEEDING` | Tốc độ > 80 km/h | HIGH | Gửi GPS 95 km/h |
| `ABNORMAL_STOP` | Động cơ bật, tốc độ ≤ 1 km/h, dừng ≥ 15 phút | MEDIUM | Kịch bản `ABNORMAL_STOP` (dừng 18 phút) |
| `ROUTE_DEVIATION` | Cách lộ trình dự kiến > 300 m | MEDIUM/HIGH | Kịch bản `ROUTE_DEVIATION` (lệch ~3 km → HIGH) |
| `CONTINUOUS_DRIVING` | Lái liên tục > 240 phút không nghỉ đủ 15 phút | MEDIUM | Kịch bản `CONTINUOUS_DRIVING` (hai mốc cách 4,5 giờ) |
| `GPS_SIGNAL_LOST` | Không có telemetry trong 10 phút | MEDIUM | Chuyến có GPS lùi 40 phút, chạy kiểm tra định kỳ |
| `DOCUMENT_EXPIRING` | Đăng kiểm/bảo hiểm/GPLX hết hạn trong 30 ngày | LOW→HIGH theo số ngày còn lại | Xe có đăng kiểm còn 10 ngày |
| `MAINTENANCE_DUE` | Số km hiện tại ≥ mốc bảo dưỡng kế tiếp | MEDIUM | Xe có odometer 20.000 km, mốc 19.000 km |

Ngưỡng của từng luật đều cấu hình được qua thuộc tính (`alerts.speed-limit-kmh`,
`alerts.route-deviation-meters`, `alerts.abnormal-stop-minutes`, `alerts.continuous-driving-minutes`,
`alerts.gps-signal-timeout-minutes`, `alerts.document-expiry-warning-days`,
`alerts.duplicate-suppression-hours`) và đã được ghi trong `application-dev.properties`/
`application-prod.properties` với giá trị mặc định dùng cho demo.

Cơ chế chống trùng: cùng loại cảnh báo trên cùng đối tượng sẽ không được tạo lại trong khoảng
`alerts.duplicate-suppression-hours` (mặc định 6 giờ) khi cảnh báo cũ vẫn đang mở, nên lịch tác vụ
chạy mỗi phút không sinh hàng loạt bản ghi trùng.

### 3.3. Kênh cảnh báo realtime (WebSocket/STOMP)

Script kiểm thử: [`scripts/verify-realtime.mjs`](../scripts/verify-realtime.mjs)

```powershell
node scripts\verify-realtime.mjs
```

Script đăng nhập lấy JWT, mở WebSocket tới `/ws?token=<JWT>`, gửi khung STOMP `CONNECT` và
`SUBSCRIBE /topic/alerts`, sau đó tạo một chuyến mới và kích hoạt cảnh báo `SPEEDING`. Kết quả thật:

```text
[INFO] Đã kích hoạt kịch bản SPEEDING cho chuyến mới RT-T-748288, đang chờ khung MESSAGE...
[PASS] Nhận cảnh báo realtime: SPEEDING/HIGH (tripId=b81840c3-8e95-4356-973d-01f2b4680bca)
```

Đặc điểm triển khai:

- Token được xác thực ngay ở bước handshake (`JwtHandshakeInterceptor`); token sai hoặc tài khoản
  không còn `ACTIVE` thì kết nối bị từ chối, không cần xử lý ở tầng ứng dụng.
- Cảnh báo chỉ được phát sau khi giao dịch commit (`@TransactionalEventListener(AFTER_COMMIT)`),
  nên client không bao giờ nhận cảnh báo của một giao dịch bị rollback.
- Dashboard vẫn giữ chu kỳ làm mới 15 giây như phương án dự phòng khi WebSocket bị chặn bởi proxy
  hoặc mất kết nối; trạng thái kết nối được hiển thị ngay trên thanh tiêu đề.

## 4. Kiểm thử cơ sở dữ liệu

Flyway và Hibernate đã được xác minh khi ứng dụng khởi động với MySQL thật:

```text
Successfully validated 5 migrations
Migrating schema `ai_transwatch` to version "1 - initial schema" ... "4 - seed demo operational data"
Successfully applied 4 migrations ... now at version v4
Initialized JPA EntityManagerFactory for persistence unit 'default'
Tomcat started on port 8080
```

Sau đó migration `V5` sửa hash mật khẩu tài khoản demo và được xác thực ở lần khởi động kế tiếp
(`Successfully validated 5 migrations`, `Current version of schema: 5`).

`spring.jpa.hibernate.ddl-auto=validate` được giữ nguyên, nghĩa là schema do Flyway tạo phải khớp
hoàn toàn với entity JPA; nếu lệch, ứng dụng sẽ dừng ngay thay vì tự sửa cấu trúc bảng.

## 5. Lỗi tích hợp đã phát hiện và khắc phục

Các lỗi dưới đây chỉ xuất hiện khi chạy thật với MySQL/AI service, không phát hiện được bằng compile:

| # | Hiện tượng | Nguyên nhân | Khắc phục |
| --- | --- | --- | --- |
| 1 | `Schema validation: missing table [alerts]` | Thiếu Spring Boot Flyway starter nên migration không tự chạy trước Hibernate | Thêm `spring-boot-starter-flyway` và `flyway-core` |
| 2 | `No qualifying bean of type ObjectMapper` | Spring Boot 4 tách nhỏ starter, thiếu JSON starter/bean tường minh | Thêm `spring-boot-starter-json` và `JsonConfig` |
| 3 | `No qualifying bean of type RestClient.Builder` | Không có auto-configuration cho RestClient | Chuyển sang `HttpClient` của JDK (xem mục 4) |
| 4 | Đăng nhập demo luôn `401` | Hash BCrypt trong seed V3 không khớp mật khẩu `password` | Thêm migration `V5` sửa hash đã kiểm chứng |
| 5 | AI trả `422 Field required: body` | JDK `HttpClient` mặc định thử nâng cấp HTTP/2 (h2c); uvicorn từ chối "upgrade request" nên phần thân yêu cầu bị mất | Ép `HttpClient.Version.HTTP_1_1` trong `AiPredictionService` |
| 6 | Preflight CORS trả `401` | Spring Security không đọc cấu hình CORS dạng `WebMvcConfigurer` | Khai báo bean `CorsConfigurationSource` và bật `http.cors(...)` |

Hai lỗi số 5 và 6 là loại lỗi chỉ lộ ra khi kiểm thử tích hợp thật; đây là nội dung đáng trình bày
trong phần kiểm thử của báo cáo.

## 6. Giới hạn của kết quả kiểm thử

- Mô hình AI được huấn luyện trên dữ liệu telemetry tổng hợp tái lập, không phải dữ liệu vận tải thật;
  kết quả chỉ mang tính minh họa kỹ thuật.
- GPS được gửi qua REST API và endpoint mô phỏng, chưa kết nối thiết bị IoT thật.
- Chưa có kiểm thử tải/đồng thời; điều phối chuyến hiện kiểm tra trùng lịch ở tầng service,
  hướng nâng cấp là khóa bi quan hoặc ràng buộc duy nhất ở mức cơ sở dữ liệu.
- Cấu hình `docker compose` trong repo chưa được chạy thử vì máy phát triển không cài Docker;
  chỉ mới kiểm tra cú pháp YAML và nội dung Dockerfile.
- Kiểm thử giao diện được thực hiện ở mức build production, kiểm tra CORS/preflight và kênh realtime;
  chưa có kiểm thử tự động trên trình duyệt (E2E UI).
