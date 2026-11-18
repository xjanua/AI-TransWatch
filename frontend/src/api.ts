const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';
const TOKEN_KEY = 'ai-transwatch.access-token';

type ApiEnvelope<T> = { success: boolean; data: T; error?: { code?: string; message?: string } };

export type CurrentUser = { id: string; username: string; email: string; fullName: string; roles: string[] };
type LoginData = { accessToken: string; tokenType: string; expiresIn: number; user: CurrentUser };
export type Trip = { id: string; tripCode: string; status: string; vehiclePlate?: string; driverName?: string; scheduledStartAt?: string; estimatedArrivalAt?: string };
export type Alert = { id: string; alertType: string; severity: string; status: string; occurredAt?: string; tripId?: string; vehicleId?: string; evidence?: string };
export type Vehicle = { id: string; licensePlate: string; status: string };
export type TransportOrder = { id: string; orderCode: string; status: string; deliveryDeadline?: string };
export type LiveVehicle = {
  tripId: string; tripCode: string; status: string; vehicleId?: string; vehiclePlate?: string; driverName?: string;
  latitude?: number | string; longitude?: number | string; speedKmh?: number | string;
  headingDegree?: number | string; engineOn?: boolean; recordedAt?: string;
};
export type PageResult<T> = { content?: T[]; items?: T[]; totalElements?: number; totalItems?: number; total?: number } | T[];

export type DailyCount = { date: string; count: number };
export type ReportOverview = {
  tripsByStatus: Record<string, number>;
  ordersByStatus: Record<string, number>;
  vehiclesByStatus: Record<string, number>;
  alertsBySeverity: Record<string, number>;
  alertsByType: Record<string, number>;
  tripsPerDay: DailyCount[];
  alertsPerDay: DailyCount[];
  onTimeRate: number;
  completedTripsWithEta: number;
  alertHandlingRate: number;
  totalAlerts: number;
  totalTrips: number;
  totalOrders: number;
};

/** Lỗi API kèm mã HTTP để tầng giao diện phân biệt 401 (hết phiên) và các lỗi khác. */
export class ApiError extends Error {
  constructor(message: string, readonly status: number) { super(message); this.name = 'ApiError'; }
}

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

async function request<T>(path: string, init: RequestInit = {}, withAuth = true): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body) headers.set('Content-Type', 'application/json; charset=utf-8');
  const token = tokenStore.get();
  if (withAuth && token) headers.set('Authorization', `Bearer ${token}`);
  const response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });
  const payload = (await response.json().catch(() => null)) as ApiEnvelope<T> | null;
  if (!response.ok || !payload?.success) {
    const message = payload?.error?.message ?? `Không thể tải dữ liệu (${response.status || 'mất kết nối'}).`;
    throw new ApiError(message, response.status);
  }
  return payload.data;
}

export const pageItems = <T,>(result: PageResult<T> | undefined): T[] =>
  Array.isArray(result) ? result : result?.content ?? result?.items ?? [];
export const pageTotal = <T,>(result: PageResult<T> | undefined): number =>
  Array.isArray(result) ? result.length : result?.totalElements ?? result?.totalItems ?? result?.total ?? pageItems(result).length;

export async function login(username: string, password: string): Promise<LoginData> {
  const data = await request<LoginData>('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }, false);
  tokenStore.set(data.accessToken);
  return data;
}

export function getCurrentUser(): Promise<CurrentUser> { return request<CurrentUser>('/auth/me'); }
export function getTrips(): Promise<PageResult<Trip>> { return request('/trips?page=0&size=50'); }
export function getAlerts(): Promise<PageResult<Alert>> { return request('/alerts?page=0&size=20'); }
export function getVehicles(): Promise<PageResult<Vehicle>> { return request('/vehicles?page=0&size=100'); }
export function getOrders(): Promise<PageResult<TransportOrder>> { return request('/orders?page=0&size=100'); }
export function getLiveFleet(): Promise<LiveVehicle[]> { return request('/monitoring/live'); }
export function getReportOverview(): Promise<ReportOverview> { return request('/reports/overview'); }

export function updateAlertStatus(id: string, status: string, handlingNote?: string): Promise<Alert> {
  return request<Alert>(`/alerts/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status, handlingNote }) });
}

export function analyzeTrip(tripId: string, body: { routeDeviationMeters?: number; stoppedMinutes?: number; remainingDistanceKm?: number }) {
  return request<{
    tripId: string; etaMinutes: number; lateProbability: number; riskLevel: string;
    anomalyScore: number; modelVersion: string; usingBaseline: boolean; explanations: string[];
  }>(`/trips/${tripId}/analysis`, { method: 'POST', body: JSON.stringify(body) });
}
