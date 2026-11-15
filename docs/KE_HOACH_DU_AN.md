# Kế hoạch dự án: AI-TransWatch

## 1. Mục tiêu
Xây dựng ứng dụng web quản lý vận tải, hỗ trợ điều phối chuyến xe và giám sát hành trình theo thời gian thực. Hệ thống tích hợp AI để dự báo thời gian đến (ETA), đánh giá nguy cơ giao trễ và phát hiện hành vi vận hành bất thường.

## 2. Phạm vi MVP

### Có trong MVP
- Đăng nhập và phân quyền: quản trị viên, điều phối viên, tài xế.
- Quản lý xe, tài xế, khách hàng, đơn vận chuyển.
- Tạo chuyến xe; phân công xe, tài xế và đơn hàng.
- Dashboard vận hành và thống kê tổng quan.
- Bản đồ theo dõi GPS giả lập theo thời gian thực.
- Cảnh báo quá tốc độ, lệch tuyến, dừng bất thường và lái xe liên tục.
- Dự báo ETA, xác suất giao trễ và mức rủi ro bằng AI.
- Thông báo realtime và lịch sử cảnh báo.
- Báo cáo hiệu suất cơ bản.

### Ngoài MVP (giai đoạn mở rộng)
- Ứng dụng mobile cho tài xế.
- Camera phát hiện buồn ngủ/mất tập trung.
- Tích hợp GPS/IoT thật, giao thông và thời tiết thật.
- Tối ưu lộ trình nhiều điểm giao.

## 3. Người dùng và quyền

| Vai trò | Quyền chính |
| --- | --- |
| Admin | Quản lý tài khoản, dữ liệu nền, cấu hình cảnh báo, xem toàn bộ báo cáo. |
| Điều phối viên | Quản lý đơn hàng, lập/phân công chuyến, theo dõi bản đồ và xử lý cảnh báo. |
| Tài xế | Xem chuyến được giao, cập nhật trạng thái và báo sự cố. |

## 4. Kịch bản demo
1. Đăng nhập bằng tài khoản điều phối viên.
2. Tạo đơn và chuyến; gán tài xế, xe và đơn hàng.
3. Bắt đầu mô phỏng hành trình trên bản đồ.
4. Xe gửi dữ liệu GPS; hệ thống tính ETA và rủi ro trễ.
5. Mô phỏng quá tốc độ, dừng lâu hoặc lệch tuyến.
6. Dashboard nhận cảnh báo realtime; điều phối viên xem chi tiết và xử lý.
7. Hoàn tất chuyến; báo cáo cập nhật hiệu suất xe và tài xế.

## 5. Kiến trúc đề xuất

```text
React + TypeScript (Frontend)
  ├─ REST API
  └─ Socket.IO / STOMP WebSocket
Spring Boot 4 + Java 21 (Backend)
  ├─ Spring Web MVC, Spring Data JPA, Spring Security
  ├─ MySQL 8 (máy chủ db.xjanua.tech:3306)
  ├─ GPS simulator / rule engine / lịch tác vụ
  └─ HTTP client tới AI service
Python FastAPI (AI service)
  ├─ ETA + late-risk prediction
  └─ anomaly detection
```

## 6. Công nghệ đề xuất

| Thành phần | Công nghệ |
| --- | --- |
| Frontend | React, TypeScript, Vite, Tailwind CSS, React Router |
| Bản đồ | Leaflet, OpenStreetMap |
| Biểu đồ | Recharts |
| Backend | Java 21, Spring Boot 4, Spring Web MVC, Spring Data JPA, Spring Security, Spring WebSocket/STOMP |
| CSDL | MySQL 8, Hibernate/JPA, Flyway |
| AI | Python, FastAPI, pandas, scikit-learn |
| Vận hành | Docker Compose |

## 7. Thiết kế dữ liệu sơ bộ

- `users`, `roles`: tài khoản và phân quyền.
- `drivers`: hồ sơ tài xế, giấy phép, điểm an toàn.
- `vehicles`: thông tin xe, tải trọng, trạng thái, chỉ số bảo dưỡng.
- `customers`: khách hàng và thông tin liên hệ.
- `transport_orders`: đơn vận chuyển, thời hạn, điểm lấy/giao.
- `trips`: chuyến xe, xe/tài xế được gán, trạng thái, thời gian.
- `trip_orders`: liên kết nhiều đơn hàng với một chuyến.
- `trip_route_points`: điểm của lộ trình dự kiến.
- `gps_logs`: lịch sử tọa độ, tốc độ, thời điểm.
- `alerts`: cảnh báo và trạng thái xử lý.
- `notifications`: thông báo realtime/lịch sử.
- `maintenance_records`: lịch sử bảo dưỡng.
- `ai_predictions`: ETA, nguy cơ trễ, anomaly score.

## 8. AI và cảnh báo

### Quy tắc vận hành
- Quá tốc độ: vận tốc vượt cấu hình của tuyến.
- Dừng bất thường: không di chuyển ngoài điểm giao/nhận quá ngưỡng.
- Lệch tuyến: khoảng cách tới lộ trình dự kiến vượt ngưỡng.
- Lái liên tục: vượt thời gian chạy liên tục cho phép.

### Mô hình AI
- ETA: Random Forest Regressor với dữ liệu quãng đường, tốc độ, giờ chạy, thời gian dừng và tốc độ lịch sử.
- Nguy cơ trễ: Random Forest Classifier, trả về xác suất và mức LOW/MEDIUM/HIGH.
- Bất thường: Isolation Forest trên đặc trưng tốc độ, gia tốc, độ lệch tuyến và thời gian dừng.
- Dữ liệu ban đầu: dữ liệu tổng hợp có script sinh và script huấn luyện tái lập.

## 9. Kế hoạch triển khai

| Giai đoạn | Kết quả bàn giao |
| --- | --- |
| 0. Nền tảng | Monorepo, cấu trúc source, Docker, schema database, ERD, dữ liệu seed. |
| 1. Backend | Auth, CRUD nghiệp vụ, API chuyến xe, kiểm thử API. |
| 2. Frontend | Layout, login, dashboard, màn hình quản lý và điều phối. |
| 3. Realtime | GPS simulator, Socket.IO, bản đồ, rule-based alerts. |
| 4. AI | Sinh/train data, FastAPI và tích hợp dự báo vào backend/UI. |
| 5. Hoàn thiện | Báo cáo, test luồng demo, README và tài liệu bảo vệ. |

## 10. Tiêu chí nghiệm thu MVP
- Khởi chạy toàn hệ thống bằng `docker compose up --build`.
- Có tài khoản demo cho từng vai trò.
- Thực hiện được trọn vẹn kịch bản demo tại mục 4.
- Có mã nguồn, file cấu hình, dữ liệu demo và hướng dẫn chạy.
- Có mô tả kiến trúc, ERD và giải thích mô hình AI.

## 11. Quyết định cần phê duyệt
Kế hoạch đã được phê duyệt sử dụng React/Spring Boot 4 + Java 21/MySQL 8/Python/Docker, giao diện tiếng Việt, GPS mô phỏng và hai nhóm AI: ETA/rủi ro trễ + bất thường hành trình. Backend kế thừa phong cách dự án Spring Boot hiện có của tác giả: cấu trúc controller/service/repository/dto/model, `RestResponse`, `BaseEntity`, MapStruct, Lombok, Springdoc và cấu hình biến môi trường. Cơ sở dữ liệu đích là máy chủ MySQL `db.xjanua.tech:3306`; thông tin xác thực chỉ được lưu cục bộ trong `.env` bị Git bỏ qua, không ghi vào tài liệu hay mã nguồn. Dự án bắt đầu từ Giai đoạn 0.

## 12. Trạng thái triển khai

Toàn bộ phạm vi MVP tại mục 2 đã được hiện thực và kiểm chứng trên MySQL thật, riêng phần
thông báo realtime đang dùng cơ chế làm mới định kỳ thay cho WebSocket:

| Hạng mục MVP | Trạng thái |
| --- | --- |
| Đăng nhập, phân quyền ba vai trò | Hoàn tất |
| Quản lý xe, tài xế, khách hàng, đơn vận chuyển | Hoàn tất |
| Tạo chuyến, phân công và kiểm tra ràng buộc | Hoàn tất |
| Dashboard vận hành và thống kê tổng quan | Hoàn tất (có tab Báo cáo với biểu đồ) |
| Bản đồ theo dõi GPS giả lập | Hoàn tất (Leaflet/OpenStreetMap, tự làm mới 15 giây) |
| Cảnh báo quá tốc độ, lệch tuyến, dừng bất thường, lái liên tục | Hoàn tất |
| Cảnh báo mất tín hiệu GPS, giấy tờ sắp hết hạn, đến hạn bảo dưỡng | Hoàn tất (lịch tác vụ định kỳ) |
| Dự báo ETA, xác suất trễ, mức rủi ro bằng AI | Hoàn tất (Random Forest + Isolation Forest) |
| Thông báo realtime và lịch sử cảnh báo | Hoàn tất (WebSocket/STOMP `/topic/alerts`, kèm làm mới định kỳ dự phòng) |
| Báo cáo hiệu suất cơ bản | Hoàn tất |

Chi tiết bằng chứng kiểm thử xem tại [Kết quả kiểm thử](KET_QUA_KIEM_THU.md).
