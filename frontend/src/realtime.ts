import { Client } from '@stomp/stompjs';
import type { Alert } from './api';

export type RealtimeStatus = 'connecting' | 'connected' | 'disconnected';

/**
 * Suy ra địa chỉ WebSocket phù hợp cho cả môi trường dev và khi chạy sau reverse proxy.
 *
 * Ưu tiên `VITE_WS_BASE_URL`; nếu không có thì lấy host từ `VITE_API_BASE_URL` khi biến này là URL
 * tuyệt đối (dev: `http://localhost:8080/api` → `ws://localhost:8080/ws`), còn lại dùng chính host
 * đang phục vụ trang web (Docker/nginx cùng origin với `/api`).
 */
function defaultWsBaseUrl(): string {
  const explicit = import.meta.env.VITE_WS_BASE_URL;
  if (explicit) return explicit;
  const apiBase = import.meta.env.VITE_API_BASE_URL;
  if (apiBase && /^https?:/i.test(apiBase)) {
    const url = new URL(apiBase);
    return `${url.protocol === 'https:' ? 'wss' : 'ws'}://${url.host}/ws`;
  }
  const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${scheme}://${window.location.host}/ws`;
}

const WS_BASE_URL = defaultWsBaseUrl();

/**
 * Kết nối kênh cảnh báo realtime qua WebSocket + STOMP.
 *
 * Backend xác thực JWT ngay ở bước handshake, nên token được truyền qua tham số truy vấn
 * (trình duyệt không đặt được header tùy biến khi mở WebSocket). Hàm trả về callback ngắt kết nối
 * để component dọn dẹp khi unmount hoặc khi đăng xuất.
 */
export function connectAlertStream(
  token: string,
  onAlert: (alert: Alert) => void,
  onStatus?: (status: RealtimeStatus) => void,
): () => void {
  const client = new Client({
    brokerURL: `${WS_BASE_URL}?token=${encodeURIComponent(token)}`,
    reconnectDelay: 5_000,
    onConnect: () => {
      onStatus?.('connected');
      client.subscribe('/topic/alerts', (message) => {
        try {
          onAlert(JSON.parse(message.body) as Alert);
        } catch {
          // Bỏ qua khung tin không hợp lệ để không làm hỏng luồng giao diện.
        }
      });
    },
    onWebSocketClose: () => onStatus?.('disconnected'),
    onStompError: () => onStatus?.('disconnected'),
  });
  onStatus?.('connecting');
  client.activate();
  return () => {
    void client.deactivate();
  };
}
