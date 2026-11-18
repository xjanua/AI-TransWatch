import {
  Activity, AlertTriangle, BarChart3, Boxes, ChevronRight, ClipboardList, LayoutDashboard, LogOut,
  MapPinned, RefreshCw, ShieldCheck, Sparkles, Truck,
} from 'lucide-react';
import { FormEvent, Suspense, lazy, useCallback, useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import {
  Alert, ApiError, CurrentUser, LiveVehicle, ReportOverview, TransportOrder, Trip, Vehicle,
  analyzeTrip, getAlerts, getCurrentUser, getLiveFleet, getOrders, getReportOverview, getTrips, getVehicles,
  login, pageItems, pageTotal, tokenStore, updateAlertStatus,
} from './api';
import { FleetMap } from './FleetMap';
import { RealtimeStatus, connectAlertStream } from './realtime';
import './styles.css';

// Tab báo cáo được tách khỏi bundle chính: biểu đồ (Recharts) chỉ tải khi người dùng mở tab,
// giúp trang đăng nhập và dashboard tải nhanh hơn.
const ReportsView = lazy(() => import('./ReportsView').then((module) => ({ default: module.ReportsView })));

/* ------------------------------------------------------------------ tiện ích */

const demo = {
  trips: [
    { id: 'd1', tripCode: 'TRIP-2026-001', status: 'IN_TRANSIT', vehiclePlate: '51D-882.19', driverName: 'Nguyễn Văn An', scheduledStartAt: new Date().toISOString() },
    { id: 'd2', tripCode: 'TRIP-2026-002', status: 'SCHEDULED', vehiclePlate: '60C-235.68', driverName: 'Trần Minh Khoa' },
  ] as Trip[],
  alerts: [
    { id: 'a1', alertType: 'LATE_RISK', severity: 'HIGH', status: 'NEW', vehicleId: '11111111-1111-1111-1111-111111111111', occurredAt: new Date().toISOString() },
    { id: 'a2', alertType: 'SPEEDING', severity: 'MEDIUM', status: 'NEW', vehicleId: '22222222-2222-2222-2222-222222222222', occurredAt: new Date().toISOString() },
  ] as Alert[],
  vehicles: [
    { id: 'v1', licensePlate: '51D-882.19', status: 'ON_TRIP' },
    { id: 'v2', licensePlate: '60C-235.68', status: 'AVAILABLE' },
  ] as Vehicle[],
  orders: [
    { id: 'o1', orderCode: 'ORD-2026-001', status: 'IN_TRANSIT' },
    { id: 'o2', orderCode: 'ORD-2026-002', status: 'PENDING' },
  ] as TransportOrder[],
};

const vn = (value?: string) =>
  value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '—';

const STATUS_LABELS: Record<string, string> = {
  SCHEDULED: 'Đã lên lịch', PICKUP_IN_PROGRESS: 'Đang lấy hàng', IN_TRANSIT: 'Đang vận chuyển',
  COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy', INCIDENT: 'Sự cố',
  NEW: 'Mới', IN_PROGRESS: 'Đang xử lý', RESOLVED: 'Đã xử lý', DISMISSED: 'Đã bỏ qua',
  SPEEDING: 'Vượt tốc độ', ABNORMAL_STOP: 'Dừng bất thường', ROUTE_DEVIATION: 'Lệch tuyến',
  LATE_RISK: 'Nguy cơ trễ', CONTINUOUS_DRIVING: 'Lái liên tục', GPS_SIGNAL_LOST: 'Mất tín hiệu GPS',
  DOCUMENT_EXPIRING: 'Giấy tờ sắp hết hạn', MAINTENANCE_DUE: 'Đến hạn bảo dưỡng', ANOMALY_DETECTED: 'Bất thường AI',
  HIGH: 'Cao', CRITICAL: 'Nghiêm trọng', MEDIUM: 'Trung bình', LOW: 'Thấp',
  AVAILABLE: 'Sẵn sàng', ON_TRIP: 'Đang chạy', MAINTENANCE: 'Bảo dưỡng', INACTIVE: 'Ngừng hoạt động',
  PENDING: 'Chờ điều phối', ASSIGNED: 'Đã phân công', DELIVERED: 'Đã giao hàng',
};

const label = (value: string) => STATUS_LABELS[value] ?? value.replaceAll('_', ' ');

/* --------------------------------------------------------------- đăng nhập */

function LoginPage({ onLogin }: { onLogin: (user: CurrentUser) => void }) {
  const navigate = useNavigate();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('password');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      const result = await login(username, password);
      onLogin(result.user);
      navigate('/dashboard', { replace: true });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Đăng nhập thất bại.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-intro">
        <p className="eyebrow">SMART LOGISTICS PLATFORM</p>
        <h1>AI-TransWatch</h1>
        <h2>Quản lý vận tải,<br />chủ động trước rủi ro.</h2>
        <p>Điều phối đội xe, theo dõi GPS và nhận cảnh báo AI ngay khi có bất thường trên hành trình.</p>
        <div className="intro-feature"><ShieldCheck /> Hệ thống giám sát vận tải thông minh</div>
      </section>
      <section className="login-card">
        <p className="eyebrow">ĐĂNG NHẬP HỆ THỐNG</p>
        <h2>Chào mừng trở lại</h2>
        <p className="muted">Đăng nhập bằng tài khoản được cấp để tiếp tục.</p>
        <form onSubmit={submit}>
          <label>Tên đăng nhập
            <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
          </label>
          <label>Mật khẩu
            <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" autoComplete="current-password" required />
          </label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="primary-button" disabled={loading}>{loading ? 'Đang xác thực...' : 'Đăng nhập'}</button>
        </form>
        <p className="demo-note">Demo: <b>admin</b> / <b>password</b></p>
      </section>
    </main>
  );
}

/* ---------------------------------------------------------------- dashboard */

type DashboardData = {
  trips: Trip[]; alerts: Alert[]; vehicles: Vehicle[]; orders: TransportOrder[];
  totals: { trips: number; alerts: number; vehicles: number; orders: number };
  fallback: boolean;
};

const EMPTY_DATA: DashboardData = {
  ...demo,
  totals: { trips: 2, alerts: 2, vehicles: 2, orders: 2 },
  fallback: true,
};

const REFRESH_INTERVAL_MS = 15_000;

function Dashboard({ user, onLogout }: { user: CurrentUser; onLogout: () => void }) {
  const [tab, setTab] = useState<'dashboard' | 'trips' | 'alerts' | 'reports'>('dashboard');
  const [data, setData] = useState<DashboardData>(EMPTY_DATA);
  const [fleet, setFleet] = useState<LiveVehicle[]>([]);
  const [report, setReport] = useState<ReportOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState('');
  const [realtime, setRealtime] = useState<RealtimeStatus>('connecting');
  const [analysis, setAnalysis] = useState<{ tripCode: string; text: string; level: string } | null>(null);

  const load = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const [t, a, v, o, live, stats] = await Promise.all([
        getTrips(), getAlerts(), getVehicles(), getOrders(), getLiveFleet(), getReportOverview(),
      ]);
      setData({
        trips: pageItems(t), alerts: pageItems(a), vehicles: pageItems(v), orders: pageItems(o),
        totals: { trips: pageTotal(t), alerts: pageTotal(a), vehicles: pageTotal(v), orders: pageTotal(o) },
        fallback: false,
      });
      setFleet(live);
      setReport(stats);
      setNotice('');
    } catch (exception) {
      if (exception instanceof ApiError && exception.status === 401) {
        onLogout();
        return;
      }
      if (exception instanceof ApiError && exception.status === 403) {
        setNotice('Tài khoản hiện tại không có quyền xem dữ liệu vận hành.');
        return;
      }
      setData(EMPTY_DATA);
      const reason = exception instanceof Error ? exception.message : 'Không thể tải dữ liệu.';
      setNotice(`${reason} Đang hiển thị dữ liệu minh họa.`);
    } finally {
      setLoading(false);
    }
  }, [onLogout]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => {
    const timer = setInterval(() => { void load(true); }, REFRESH_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [load]);

  // Kênh cảnh báo realtime: cảnh báo mới được đẩy ngay lên giao diện, không phải chờ chu kỳ làm mới.
  useEffect(() => {
    const token = tokenStore.get();
    if (!token) return;
    return connectAlertStream(token, (alert) => {
      setData((previous) => ({
        ...previous,
        alerts: [alert, ...previous.alerts.filter((item) => item.id !== alert.id)].slice(0, 20),
        totals: { ...previous.totals, alerts: previous.totals.alerts + 1 },
      }));
      setNotice(`Cảnh báo mới: ${label(alert.alertType)} · mức ${label(alert.severity)}`);
    }, setRealtime);
  }, []);

  const running = useMemo(() => data.vehicles.filter((v) => v.status === 'ON_TRIP').length, [data.vehicles]);
  const openAlerts = useMemo(() => data.alerts.filter((a) => ['NEW', 'IN_PROGRESS'].includes(a.status)), [data.alerts]);
  const highAlerts = openAlerts.filter((a) => ['HIGH', 'CRITICAL'].includes(a.severity)).length;

  const metrics = [
    { label: 'Xe đang vận hành', value: running, icon: Truck, note: `${data.totals.vehicles} xe trong hệ thống` },
    { label: 'Chuyến theo dõi', value: data.totals.trips, icon: MapPinned, note: 'Cập nhật mỗi 15 giây' },
    { label: 'Cảnh báo mở', value: openAlerts.length, icon: AlertTriangle, note: `${highAlerts} mức độ cao` },
    { label: 'Đơn vận chuyển', value: data.totals.orders, icon: Boxes, note: `${data.orders.filter((o) => o.status === 'PENDING').length} đơn chờ điều phối` },
  ];

  const activeTrip = fleet.find((v) => v.tripCode)?.tripId;

  async function runAnalysis(tripId: string, tripCode: string) {
    setAnalysis({ tripCode, level: 'PENDING', text: 'Đang gọi mô hình AI...' });
    try {
      const result = await analyzeTrip(tripId, { routeDeviationMeters: 320, stoppedMinutes: 12, remainingDistanceKm: 25 });
      setAnalysis({
        tripCode,
        level: result.riskLevel,
        text: `ETA ${Number(result.etaMinutes).toFixed(0)} phút · nguy cơ trễ ${(result.lateProbability * 100).toFixed(0)}% · ${result.explanations[0]}`,
      });
    } catch (exception) {
      setAnalysis({ tripCode, level: 'ERROR', text: exception instanceof Error ? exception.message : 'Không gọi được AI service.' });
    }
  }

  async function handleAlert(alertId: string, status: string) {
    try {
      await updateAlertStatus(alertId, status, status === 'RESOLVED' ? 'Đã xử lý từ dashboard' : undefined);
      await load(true);
    } catch (exception) {
      setNotice(exception instanceof Error ? exception.message : 'Không cập nhật được cảnh báo.');
    }
  }

  return (
    <main className="app-layout">
      <aside className="sidebar">
        <div className="brand"><span>AT</span><div><b>AI-TransWatch</b><small>SMART LOGISTICS</small></div></div>
        <nav>
          <button className={tab === 'dashboard' ? 'active' : ''} onClick={() => setTab('dashboard')}><LayoutDashboard /> Tổng quan</button>
          <button className={tab === 'trips' ? 'active' : ''} onClick={() => setTab('trips')}><Truck /> Chuyến xe</button>
          <button className={tab === 'alerts' ? 'active' : ''} onClick={() => setTab('alerts')}><AlertTriangle /> Cảnh báo <em>{openAlerts.length}</em></button>
          <button className={tab === 'reports' ? 'active' : ''} onClick={() => setTab('reports')}><BarChart3 /> Báo cáo</button>
        </nav>
        <div className="sidebar-foot">
          <b>{user.fullName}</b>
          <small>{user.roles.map(label).join(' · ')}</small>
          <button onClick={onLogout}><LogOut /> Đăng xuất</button>
        </div>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">SMART LOGISTICS</p>
            <h1>{tab === 'dashboard' ? 'Bảng điều hành' : tab === 'trips' ? 'Chuyến vận chuyển' : tab === 'alerts' ? 'Trung tâm cảnh báo' : 'Báo cáo thống kê'}</h1>
          </div>
          <div className="top-actions">
            <span className={`status realtime-${realtime}`}>
              <i /> {data.fallback ? 'Chế độ minh họa' : realtime === 'connected' ? 'Realtime đã kết nối' : realtime === 'connecting' ? 'Đang kết nối realtime' : 'Đồng bộ định kỳ'}
            </span>
            <button className="icon-button" onClick={() => void load()} disabled={loading} title="Làm mới">
              <RefreshCw size={18} className={loading ? 'spin' : ''} />
            </button>
          </div>
        </header>

        {notice && <div className="offline-note">{notice}</div>}

        {tab === 'dashboard' && (
          <>
            <section className="hero compact">
              <div>
                <p className="eyebrow">DASHBOARD VẬN HÀNH</p>
                <h2>Giám sát đội xe thông minh,<br />xử lý rủi ro chủ động.</h2>
                <p>Dữ liệu tổng hợp từ chuyến xe, GPS, cảnh báo và mô hình AI.</p>
              </div>
              <button className="primary-button" onClick={() => void load()} disabled={loading}>Làm mới dữ liệu</button>
            </section>

            <section className="grid">
              {metrics.map(({ label: title, value, icon: Icon, note }) => (
                <article className="metric" key={title}>
                  <Icon /><p>{title}</p><strong>{value}</strong><small>{note}</small>
                </article>
              ))}
            </section>

            <section className="panels">
              <article className="map">
                <div className="section-heading">
                  <div><p className="eyebrow">GIÁM SÁT TRỰC TUYẾN</p><h3>Đội xe trên hành trình</h3></div>
                  <span className="muted">{fleet.filter((v) => v.latitude != null).length}/{fleet.length} xe có GPS</span>
                </div>
                <FleetMap vehicles={fleet} />
                {activeTrip && (
                  <div className="analysis-row">
                    <button className="text-button" onClick={() => void runAnalysis(activeTrip, fleet.find((v) => v.tripId === activeTrip)!.tripCode)}>
                      <Sparkles size={16} /> Phân tích AI chuyến đang chạy
                    </button>
                    {analysis && (
                      <span className={`analysis-result level-${analysis.level.toLowerCase()}`}>
                        <b>{analysis.tripCode}</b> · {analysis.text}
                      </span>
                    )}
                  </div>
                )}
              </article>

              <AlertList alerts={data.alerts.slice(0, 5)} compact onMore={() => setTab('alerts')} onHandle={handleAlert} />
            </section>
          </>
        )}

        {tab === 'trips' && <TripList trips={data.trips} onAnalyze={runAnalysis} />}
        {tab === 'alerts' && <AlertList alerts={data.alerts} onHandle={handleAlert} />}
        {tab === 'reports' && (
          <Suspense fallback={<section className="data-card"><p className="muted">Đang tải biểu đồ báo cáo...</p></section>}>
            <ReportsView report={report} />
          </Suspense>
        )}
      </section>
    </main>
  );
}

/* -------------------------------------------------------------- danh sách */

function TripList({ trips, onAnalyze }: { trips: Trip[]; onAnalyze: (tripId: string, tripCode: string) => void }) {
  return (
    <section className="data-card">
      <div className="section-heading">
        <div><p className="eyebrow">ĐIỀU PHỐI</p><h2>Danh sách chuyến xe</h2></div>
        <span className="muted">{trips.length} chuyến</span>
      </div>
      {trips.length ? (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Mã chuyến</th><th>Xe</th><th>Tài xế</th><th>Khởi hành</th><th>Trạng thái</th><th>AI</th></tr></thead>
            <tbody>
              {trips.map((t) => (
                <tr key={t.id}>
                  <td><b>{t.tripCode}</b></td>
                  <td>{t.vehiclePlate ?? '—'}</td>
                  <td>{t.driverName ?? '—'}</td>
                  <td>{vn(t.scheduledStartAt)}</td>
                  <td><span className={`badge ${t.status.toLowerCase()}`}>{label(t.status)}</span></td>
                  <td><button className="text-button" onClick={() => onAnalyze(t.id, t.tripCode)}><Activity size={15} /> Phân tích</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : <Empty icon={<Truck />} text="Chưa có chuyến xe nào." />}
    </section>
  );
}

function AlertList({ alerts, compact = false, onMore, onHandle }: {
  alerts: Alert[]; compact?: boolean; onMore?: () => void; onHandle: (alertId: string, status: string) => void;
}) {
  return (
    <article className={compact ? 'alerts' : 'data-card'}>
      <div className="section-heading">
        <div><p className="eyebrow">CẦN XỬ LÝ</p><h2>{compact ? 'Cảnh báo mới nhất' : 'Danh sách cảnh báo'}</h2></div>
        {compact && <button className="text-button" onClick={onMore}>Xem tất cả <ChevronRight size={16} /></button>}
      </div>
      {alerts.length ? (
        <ul className="alert-list">
          {alerts.map((a) => (
            <li key={a.id}>
              <span className={`severity ${a.severity.toLowerCase()}`}><AlertTriangle size={16} /></span>
              <div>
                <b>{label(a.alertType)}</b>
                <small>
                  {a.vehicleId ? `Xe #${a.vehicleId.slice(0, 8)}` : a.tripId ? `Chuyến #${a.tripId.slice(0, 8)}` : 'Hệ thống'} · {vn(a.occurredAt)}
                </small>
                <span className="alert-actions">
                  <button className="text-button" onClick={() => onHandle(a.id, 'IN_PROGRESS')}>Nhận xử lý</button>
                  <button className="text-button" onClick={() => onHandle(a.id, 'RESOLVED')}>Đã xử lý</button>
                </span>
              </div>
              <span className={`badge ${a.severity.toLowerCase()}`}>{label(a.severity)}</span>
            </li>
          ))}
        </ul>
      ) : <Empty icon={<ClipboardList />} text="Không có cảnh báo cần xử lý." />}
    </article>
  );
}

function Empty({ icon, text }: { icon: React.ReactNode; text: string }) {
  return <div className="empty">{icon}<p>{text}</p></div>;
}

/* -------------------------------------------------------------------- app */

function App() {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    if (!tokenStore.get()) { setChecking(false); return; }
    getCurrentUser()
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setChecking(false));
  }, []);

  const logout = () => { tokenStore.clear(); setUser(null); };

  if (checking) return <main className="loading-screen">Đang kiểm tra phiên đăng nhập...</main>;

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/dashboard" replace /> : <LoginPage onLogin={setUser} />} />
      <Route path="/dashboard" element={user ? <Dashboard user={user} onLogout={logout} /> : <Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to={user ? '/dashboard' : '/login'} replace />} />
    </Routes>
  );
}

createRoot(document.getElementById('root')!).render(<BrowserRouter><App /></BrowserRouter>);
