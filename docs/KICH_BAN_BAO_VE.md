# Kịch bản bảo vệ đồ án — AI-TransWatch (8–10 phút)

## 1. Mục tiêu buổi trình bày

Chứng minh hệ thống giải quyết một quy trình vận tải xuyên suốt: quản lý dữ liệu đầu vào, điều phối xe/tài xế/đơn, nhận GPS, cảnh báo rule-based, phân tích AI và hiển thị kết quả cho điều phối viên.

> **Nguyên tắc an toàn khi demo:** dùng database demo/local, không hiển thị file `.env`, mật khẩu MySQL, JWT secret hoặc token trên màn chiếu. Tài khoản `password` chỉ là seed phục vụ bảo vệ. Không mô tả model tổng hợp là mô hình đã đủ điều kiện vận hành thương mại.

## 2. Chuẩn bị trước khi bảo vệ (10–15 phút)

1. Làm theo [Hướng dẫn chạy](HUONG_DAN_CHAY.md) để chạy MySQL, AI service (`http://127.0.0.1:8000`), Spring Boot (`http://127.0.0.1:8080`) và frontend (`http://localhost:5173`).
2. Train model AI và kiểm tra FastAPI health.
3. Mở sẵn các tab:
   - Frontend: [http://localhost:5173](http://localhost:5173)
   - Backend Swagger: [http://127.0.0.1:8080/swagger-ui.html](http://127.0.0.1:8080/swagger-ui.html)
   - AI Swagger: [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs)
4. Đăng nhập frontend bằng `dispatcher` / `password` (hoặc dùng `admin` / `password`).
5. Chuẩn bị dữ liệu tối thiểu bằng API/UI: một khách hàng `ACTIVE`, một xe `AVAILABLE`, một tài xế `AVAILABLE`, và một hay nhiều đơn `PENDING`. Tên/mã gợi ý: `KH-DEMO-01`, `51D-882.19`, `TX-DEMO-01`, `DH-DEMO-01`.
6. Để dự phòng lỗi Internet, demo chỉ dùng API local. MySQL đã được xác minh kết nối tới `db.xjanua.tech:3306` (schema `ai_transwatch`); nếu mạng phòng bảo vệ chặn cổng 3306, chuyển sang MySQL local đã chạy migration `dev` và nói rõ với hội đồng.

## 3. Phân bổ thời gian và lời dẫn

| Thời lượng | Nội dung | Kết quả cần thấy |
| --- | --- | --- |
| 0:00–1:00 | Bài toán, mục tiêu và kiến trúc | Nêu rõ Web React – Spring Boot/MySQL – FastAPI AI |
| 1:00–2:00 | Đăng nhập, phân quyền, dashboard | Điều phối viên đăng nhập và dashboard tải dữ liệu |
| 2:00–3:30 | Dữ liệu nền và đơn vận chuyển | Xe/tài xế/khách hàng/đơn có trạng thái hợp lệ |
| 3:30–5:15 | Điều phối chuyến xe | Tạo chuyến, kiểm tra tải trọng/trạng thái/lịch |
| 5:15–6:45 | GPS và cảnh báo | Gửi telemetry bất thường, xuất hiện alert |
| 6:45–8:15 | AI dự báo và giải thích | ETA, late probability, risk/anomaly, explanation |
| 8:15–9:15 | Xử lý alert và kết quả | Alert chuyển trạng thái, nêu giá trị vận hành |
| 9:15–10:00 | Báo cáo, giới hạn, đóng góp và hướng phát triển | Tab Báo cáo có số liệu thật; nêu trung thực dữ liệu synthetic và lộ trình IoT/realtime |

## 4. Trình tự demo chi tiết

### Bước 1 — Bài toán và kiến trúc (0:00–1:00)

**Lời dẫn gợi ý:**

> AI-TransWatch hỗ trợ điều phối viên nhìn thấy toàn bộ tài nguyên vận tải, đồng thời phát hiện sớm chuyến có nguy cơ chậm hoặc vận hành bất thường. Frontend React hiển thị vận hành; Spring Boot quản lý nghiệp vụ, phân quyền và MySQL; FastAPI phục vụ các mô hình ML.

Mở [Kế hoạch dự án](KE_HOACH_DU_AN.md) hoặc [ERD](ERD.md) nếu cần minh họa bảng `trips`, `gps_logs`, `alerts`, `ai_predictions`.

### Bước 2 — Xác thực và dashboard (1:00–2:00)

1. Mở frontend, vào `/login`.
2. Đăng nhập `dispatcher` / `password`.
3. Cho hội đồng xem dashboard: KPI xe/chuyến/đơn/cảnh báo, các tab Tổng quan, Chuyến xe và Cảnh báo.
4. Có thể mở Swagger và gọi `GET /api/auth/me` để chứng minh backend trả role `DISPATCHER` qua JWT.

**Điểm nhấn:** frontend giữ token ở localStorage trong phạm vi MVP; Spring Security xác thực Bearer JWT và kiểm soát quyền API.

### Bước 3 — Chuẩn bị dữ liệu và tạo đơn (2:00–3:30)

Nếu chưa seed nghiệp vụ, dùng Swagger với JWT `dispatcher`:

1. Tạo khách hàng qua `POST /api/customers` với `status: ACTIVE`.
2. Tạo xe qua `POST /api/vehicles` với `status: AVAILABLE`, `maxPayloadKg` đủ lớn.
3. Tạo tài xế qua `POST /api/drivers` với `status: AVAILABLE`, giấy phép còn hạn.
4. Tạo đơn qua `POST /api/orders` với `status: PENDING`, `weightKg` dương, khách hàng đang `ACTIVE`.

**Lời dẫn:** hệ thống kiểm tra khách hàng hợp lệ, mã đơn duy nhất, khối lượng dương, cặp tọa độ hợp lệ và deadline không nằm trong quá khứ.

### Bước 4 — Điều phối chuyến xe (3:30–5:15)

Dùng `POST /api/trips` với mã chuyến, `vehicleId`, `driverId`, danh sách `orderIds`, `scheduledStartAt` và `estimatedArrivalAt`.

**Kỳ vọng:**

- Chuyến mới ở `SCHEDULED`.
- Xe/tài xế được chuyển sang `ON_TRIP`.
- Các đơn chuyển sang `ASSIGNED`.

**Nêu ràng buộc:** chỉ nhận xe/tài xế `AVAILABLE`; tổng `weightKg` không vượt tải trọng xe; đơn phải `PENDING`; hệ thống chặn trùng lịch của xe/tài xế.

Có thể minh họa kiểm tra bằng cách cố gắng chọn xe quá tải hoặc xe đang `ON_TRIP`; API phải từ chối với thông báo nghiệp vụ thay vì tạo dữ liệu sai.

### Bước 5 — Gửi GPS và tạo cảnh báo (5:15–6:45)

Chuyển chuyến sang `PICKUP_IN_PROGRESS`, sau đó `IN_TRANSIT` qua:

```http
PATCH /api/trips/{tripId}/status
{"status":"PICKUP_IN_PROGRESS"}

PATCH /api/trips/{tripId}/status
{"status":"IN_TRANSIT"}
```

Gửi GPS vượt tốc độ bằng `POST /api/trips/{tripId}/gps`:

```json
{
  "latitude": 10.7769,
  "longitude": 106.7009,
  "speedKmh": 95,
  "headingDegree": 90,
  "engineOn": true,
  "stoppedMinutes": 0
}
```

> **Cách nhanh và ổn định hơn cho demo:** dùng endpoint mô phỏng chỉ dành cho admin, mỗi lần gọi
> tạo telemetry với kịch bản xác định:
>
> ```http
> POST /api/demo/trips/{tripId}/simulate-step
> {"scenario":"SPEEDING"}
> ```
>
> | Kịch bản | Kết quả cảnh báo |
> | --- | --- |
> | `NORMAL` | Không sinh cảnh báo |
> | `SPEEDING` | `SPEEDING/HIGH` (92 km/h) |
> | `ABNORMAL_STOP` | `ABNORMAL_STOP/MEDIUM` (0 km/h, dừng 18 phút) |
> | `ROUTE_DEVIATION` | `ROUTE_DEVIATION/HIGH` (lệch ~3 km khỏi lộ trình) |
> | `CONTINUOUS_DRIVING` | `CONTINUOUS_DRIVING/MEDIUM` (hai mốc cách 4,5 giờ, không nghỉ đủ) |
>
> Endpoint chỉ tồn tại ở profile `dev`/`demo`. Dữ liệu seed có sẵn chuyến `TRIP-DEMO-001`
> (đang `IN_TRANSIT`) cùng hai điểm lộ trình để dùng ngay.
>
> Để minh họa cảnh báo định kỳ (mất tín hiệu GPS, giấy tờ sắp hết hạn, đến hạn bảo dưỡng), gọi
> `POST /api/demo/trips/alerts/run-checks` thay vì chờ lịch tác vụ 60 giây.
>
> **Minh họa realtime:** mở dashboard ở một tab, dùng Swagger hoặc tab thứ hai kích hoạt kịch bản
> `SPEEDING`; cảnh báo xuất hiện ngay trên danh sách và thanh trạng thái hiển thị
> “Realtime đã kết nối” — không phải chờ chu kỳ làm mới 15 giây.

Sau đó mở tab Cảnh báo hoặc gọi `GET /api/alerts?tripId={tripId}`. Có thể mở tab Tổng quan để xem
bản đồ Leaflet/OpenStreetMap hiển thị vị trí đội xe (dữ liệu từ `GET /api/monitoring/live`, tự động
làm mới mỗi 15 giây) và bấm "Phân tích AI chuyến đang chạy" để minh họa kết quả mô hình ngay trên dashboard.

**Kỳ vọng:** xuất hiện `SPEEDING`, severity `HIGH`, evidence JSON chứa tốc độ và ngưỡng 80 km/h.

Tùy chọn, minh họa dừng bất thường:

```json
{
  "latitude": 10.7769,
  "longitude": 106.7009,
  "speedKmh": 0,
  "engineOn": true,
  "stoppedMinutes": 20
}
```

**Kỳ vọng:** `ABNORMAL_STOP`, severity `MEDIUM`.

### Bước 6 — Gọi AI phân tích chuyến (6:45–8:15)

Sau khi có GPS, gọi:

```http
POST /api/trips/{tripId}/analysis
Content-Type: application/json

{
  "routeDeviationMeters": 450,
  "stoppedMinutes": 20,
  "remainingDistanceKm": 35
}
```

**Kỳ vọng:** backend lấy GPS gần nhất, gọi FastAPI `/v1/predictions/analyze`, trả `etaMinutes`, `lateProbability`, `riskLevel`, `anomalyScore`, `modelVersion` và `explanations` tiếng Việt.

**Lời dẫn về AI:**

> ETA dùng Random Forest Regressor; xác suất trễ dùng Random Forest Classifier; hành vi bất thường dùng Isolation Forest. Model hiện train trên dữ liệu telemetry tổng hợp tái lập để phục vụ đồ án. Khi có dữ liệu GPS thực, pipeline sẽ được train/đánh giá lại trước khi ứng dụng vận hành thật.

Nếu AI service lỗi, API backend phải trả `503`; đây là cơ chế fail-fast rõ ràng, không giả mạo kết quả AI.

### Bước 7 — Xử lý cảnh báo và hoàn tất (8:15–9:15)

1. Trên Alert Center, chọn alert và cập nhật qua `PATCH /api/alerts/{alertId}/status`, ví dụ:

   ```json
   {"status":"IN_PROGRESS","handlingNote":"Đã liên hệ tài xế, yêu cầu giảm tốc và kiểm tra lộ trình."}
   ```

2. Nhấn mạnh lịch sử evidence, người xử lý và ghi chú giúp truy vết.
3. Có thể hoàn tất chuyến: `PATCH /api/trips/{tripId}/status` với `COMPLETED`.

**Kỳ vọng:** đơn chuyển `DELIVERED`, xe/tài xế trở lại `AVAILABLE`.

### Bước 8 — Báo cáo, kết luận và hướng phát triển (9:15–10:00)

Mở tab **Báo cáo** để trình bày số liệu tổng hợp thật từ MySQL: tỷ lệ giao đúng hạn, tỷ lệ xử lý cảnh báo,
biểu đồ chuyến xe/cảnh báo theo ngày (7 ngày gần nhất), cảnh báo theo mức độ và phân bổ trạng thái.

Chốt bằng ba đóng góp:

1. Quản trị và điều phối vận tải có ràng buộc nghiệp vụ.
2. Giám sát telemetry và cảnh báo có thể giải thích.
3. AI dự báo ETA/rủi ro và phát hiện bất thường được tách riêng thành service, dễ thay model.

Hướng phát triển: GPS/IoT thật, Leaflet/OSM với route thực, STOMP/WebSocket realtime, dữ liệu giao thông/thời tiết, tối ưu đa điểm, mobile cho tài xế và camera phát hiện buồn ngủ.

## 5. Phương án dự phòng khi demo lỗi

| Tình huống | Cách xử lý ngắn gọn |
| --- | --- |
| Frontend không tải API | Chuyển sang Swagger để trình diễn endpoint, kiểm tra `VITE_API_BASE_URL` và CORS. |
| Backend không kết nối MySQL remote | Dùng MySQL local đã chạy migration `dev`; nói rõ cổng 3306 có thể bị chặn ở mạng phòng bảo vệ. |
| AI service tắt | Mở terminal `ai-service`, chạy uvicorn; nếu không kịp, trình bày endpoint `/health`, code/Swagger và giải thích backend trả 503 có kiểm soát. |
| Chưa có GPS nên analysis trả 409 | Gửi một telemetry GPS hợp lệ trước, sau đó gọi `/analysis`. |
| Tài khoản demo không login | Kiểm tra Flyway V3 đã chạy; dùng database demo sạch, không thay hash mật khẩu. |
| Mạng Internet không ổn định | Không phụ thuộc API ngoài; FastAPI/MySQL/frontend đều chạy local. |

## 6. Câu hỏi thường gặp từ hội đồng

- **AI có phải chỉ là if/else?** Không. Rule engine xử lý sự kiện xác định như tốc độ > 80 km/h; còn `/analysis` sử dụng Random Forest và Isolation Forest. Baseline chỉ là phương án fallback minh bạch khi artifact chưa train.
- **Dữ liệu train từ đâu?** Phiên bản đồ án dùng generator telemetry tổng hợp tái lập. Đây là giới hạn được nêu rõ; triển khai thật phải dùng dữ liệu lịch sử đã làm sạch và đánh giá model.
- **Làm sao tránh điều phối sai?** Service kiểm tra trạng thái tài nguyên, tải trọng, trạng thái đơn và xung đột thời gian trước khi tạo chuyến.
- **Bảo mật thế nào?** Spring Security dùng JWT Bearer, role Admin/Dispatcher/Driver; secret chỉ ở `.env`, password demo chỉ được dùng trong môi trường bảo vệ.
- **Khả năng mở rộng?** Frontend, Spring Boot và FastAPI tách service; Flyway quản lý schema, API có phân trang; GPS log và prediction được thiết kế lưu lịch sử.
