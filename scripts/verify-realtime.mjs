// ============================================================================
//  Kiểm thử kênh cảnh báo realtime của AI-TransWatch.
//
//  Cách chạy (backend đang chạy ở cổng 8080):
//    cd D:\Programing\DACN ; node scripts/verify-realtime.mjs
//
//  Script dùng WebSocket có sẵn trong Node 22+ và tự gửi khung STOMP tối thiểu:
//    1. Đăng nhập lấy JWT.
//    2. Mở WebSocket tới /ws?token=<JWT>, gửi CONNECT rồi SUBSCRIBE /topic/alerts.
//    3. Kích hoạt một cảnh báo bằng endpoint mô phỏng (SPEEDING) và chờ khung MESSAGE.
//  Thoát 0 nếu nhận được cảnh báo, thoát 1 nếu không.
// ============================================================================
const API_BASE = process.env.API_BASE ?? 'http://127.0.0.1:8080/api';
const WS_BASE = process.env.WS_BASE ?? 'ws://127.0.0.1:8080/ws';
const TIMEOUT_MS = 20_000;

const frame = (command, headers, body = '') =>
  `${command}\n${Object.entries(headers).map(([key, value]) => `${key}:${value}`).join('\n')}\n\n${body}\0`;

async function login() {
  const response = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'password' }),
  });
  const payload = await response.json();
  if (!payload?.success) throw new Error(`Đăng nhập thất bại: ${JSON.stringify(payload)}`);
  return payload.data.accessToken;
}

/** Định dạng thời gian theo giờ địa phương (backend nhận LocalDateTime, không nhận UTC 'Z'). */
const localDateTime = (date) => new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 19);

async function post(token, path, body) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json; charset=utf-8' },
    body: JSON.stringify(body),
  });
  const payload = await response.json();
  if (!response.ok || !payload?.success) throw new Error(`POST ${path} thất bại: ${JSON.stringify(payload)}`);
  return payload.data;
}

/**
 * Tạo chuyến mới hoàn toàn cho mỗi lần kiểm thử.
 *
 * Không dùng chuyến seed vì cơ chế chống trùng cảnh báo (6 giờ) sẽ chặn cảnh báo SPEEDING
 * nếu chuyến đó vừa phát sinh cảnh báo trong các lần chạy trước.
 */
async function createFreshTrip(token) {
  const stamp = Date.now().toString().slice(-6);
  const headers = { Authorization: `Bearer ${token}` };
  const customers = await (await fetch(`${API_BASE}/customers?page=0&size=1`, { headers })).json();
  const customerId = customers.data.items[0].id;

  const vehicle = await post(token, '/vehicles', {
    licensePlate: `RT-${stamp}`, vehicleType: 'TRUCK', maxPayloadKg: 5000, currentOdometerKm: 500, status: 'AVAILABLE',
  });
  const driver = await post(token, '/drivers', {
    employeeCode: `RT-D-${stamp}`, licenseNumber: `RT-L-${stamp}`, licenseClass: 'C',
    licenseExpiryDate: '2030-12-31', status: 'AVAILABLE', safetyScore: 95, totalDistanceKm: 0,
  });
  const order = await post(token, '/orders', {
    orderCode: `RT-O-${stamp}`, customerId, cargoName: 'Realtime test', weightKg: 100,
    pickupAddress: 'A', deliveryAddress: 'B',
  });
  const trip = await post(token, '/trips', {
    tripCode: `RT-T-${stamp}`, vehicleId: vehicle.id, driverId: driver.id, orderIds: [order.id],
    scheduledStartAt: localDateTime(new Date(Date.now() + 120_000)),
  });
  return trip;
}

async function triggerAlert(token, trip) {
  await fetch(`${API_BASE}/trips/${trip.id}/status`, {
    method: 'PATCH',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ status: 'PICKUP_IN_PROGRESS' }),
  });
  await post(token, `/demo/trips/${trip.id}/simulate-step`, { scenario: 'SPEEDING' });
  return trip.tripCode;
}

async function main() {
  const token = await login();
  const socket = new WebSocket(`${WS_BASE}?token=${encodeURIComponent(token)}`);
  const received = [];
  let settled = false;

  const finish = (code, message) => {
    if (settled) return;
    settled = true;
    console.log(message);
    try { socket.close(); } catch { /* bỏ qua */ }
    process.exit(code);
  };

  socket.addEventListener('open', () => {
    socket.send(frame('CONNECT', { 'accept-version': '1.2', host: 'localhost' }));
  });

  socket.addEventListener('message', async (event) => {
    const text = typeof event.data === 'string' ? event.data : event.data.toString();
    if (text.startsWith('CONNECTED')) {
      socket.send(frame('SUBSCRIBE', { id: 'alerts-sub', destination: '/topic/alerts' }));
      try {
        const trip = await createFreshTrip(token);
        const tripCode = await triggerAlert(token, trip);
        console.log(`  [INFO] Đã kích hoạt kịch bản SPEEDING cho chuyến mới ${tripCode}, đang chờ khung MESSAGE...`);
      } catch (error) {
        finish(1, `  [FAIL] ${error.message}`);
      }
      return;
    }
    if (text.startsWith('MESSAGE')) {
      const body = text.slice(text.indexOf('\n\n') + 2).replace(/\0$/, '');
      received.push(body);
      try {
        const alert = JSON.parse(body);
        finish(0, `  [PASS] Nhận cảnh báo realtime: ${alert.alertType}/${alert.severity} (tripId=${alert.tripId})`);
      } catch {
        finish(0, `  [PASS] Nhận khung MESSAGE realtime: ${body.slice(0, 120)}`);
      }
      return;
    }
    if (text.startsWith('ERROR')) {
      finish(1, `  [FAIL] Broker trả ERROR: ${text.replace(/\0$/, '')}`);
    }
  });

  socket.addEventListener('error', () => finish(1, '  [FAIL] Không mở được WebSocket (kiểm tra backend và token)'));

  setTimeout(() => finish(1, `  [FAIL] Hết thời gian chờ, không nhận được cảnh báo realtime (nhận ${received.length} khung)`), TIMEOUT_MS);
}

main().catch((error) => {
  console.error(`  [FAIL] ${error.message}`);
  process.exit(1);
});
