import { AlertTriangle } from 'lucide-react';
import {
  Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import type { ReportOverview } from './api';

const SEVERITY_COLORS: Record<string, string> = {
  LOW: '#55dc9e', MEDIUM: '#ffc177', HIGH: '#ff8b7a', CRITICAL: '#ff5f4d',
};

const STATUS_COLORS = ['#66e2bf', '#63b5f5', '#ffc177', '#ff8b7a', '#9f8cf0', '#8da8a9'];

const dayLabel = (value: string) => new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });

/** Biểu đồ và chỉ số tổng hợp cho tab báo cáo. */
export function ReportsView({ report }: { report: ReportOverview | null }) {
  if (!report) {
    return <section className="data-card"><p className="muted">Chưa tải được số liệu báo cáo.</p></section>;
  }

  const trend = report.tripsPerDay.map((item, index) => ({
    date: dayLabel(item.date),
    'Chuyến xe': item.count,
    'Cảnh báo': report.alertsPerDay[index]?.count ?? 0,
  }));

  const severities = Object.entries(report.alertsBySeverity).map(([name, value]) => ({ name, value }));
  const alertTypes = Object.entries(report.alertsByType).map(([name, value]) => ({ name, value }));
  const statusGroups: Array<[string, Record<string, number>]> = [
    ['Chuyến xe', report.tripsByStatus],
    ['Đơn vận chuyển', report.ordersByStatus],
    ['Phương tiện', report.vehiclesByStatus],
  ];

  return (
    <>
      <section className="grid">
        <article className="metric"><p>Tỷ lệ giao đúng hạn</p><strong>{(report.onTimeRate * 100).toFixed(1)}%</strong><small>{report.completedTripsWithEta} chuyến có dữ liệu ETA</small></article>
        <article className="metric"><p>Tỷ lệ xử lý cảnh báo</p><strong>{(report.alertHandlingRate * 100).toFixed(0)}%</strong><small>{report.totalAlerts} cảnh báo phát sinh</small></article>
        <article className="metric"><p>Tổng chuyến xe</p><strong>{report.totalTrips}</strong><small>Toàn bộ lịch sử</small></article>
        <article className="metric"><p>Tổng đơn vận chuyển</p><strong>{report.totalOrders}</strong><small>Toàn bộ lịch sử</small></article>
      </section>

      <section className="panels">
        <article>
          <div className="section-heading">
            <div><p className="eyebrow">XU HƯỚNG 7 NGÀY</p><h3>Chuyến xe và cảnh báo theo ngày</h3></div>
          </div>
          <div className="chart">
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={trend}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1a3639" vertical={false} />
                <XAxis dataKey="date" stroke="#8da8a9" fontSize={12} />
                <YAxis stroke="#8da8a9" fontSize={12} allowDecimals={false} />
                <Tooltip contentStyle={{ background: '#0d1d20', border: '1px solid #254146', borderRadius: 10 }} />
                <Legend />
                <Bar dataKey="Chuyến xe" fill="#66e2bf" radius={[4, 4, 0, 0]} />
                <Bar dataKey="Cảnh báo" fill="#ffc177" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </article>

        <article>
          <div className="section-heading">
            <div><p className="eyebrow">MỨC ĐỘ</p><h3>Cảnh báo theo mức độ</h3></div>
          </div>
          {severities.length ? (
            <div className="chart">
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie data={severities} dataKey="value" nameKey="name" innerRadius={55} outerRadius={95} paddingAngle={3}>
                    {severities.map((entry) => (
                      <Cell key={entry.name} fill={SEVERITY_COLORS[entry.name] ?? '#66e2bf'} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ background: '#0d1d20', border: '1px solid #254146', borderRadius: 10 }} />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
          ) : <p className="muted">Chưa có cảnh báo nào được ghi nhận.</p>}
        </article>
      </section>

      <section className="panels">
        <article className="data-card">
          <div className="section-heading"><div><p className="eyebrow">PHÂN BỔ</p><h3>Trạng thái theo nhóm dữ liệu</h3></div></div>
          <div className="status-groups">
            {statusGroups.map(([title, values]) => {
              const total = Object.values(values).reduce((a, b) => a + b, 0);
              return (
                <div key={title}>
                  <b>{title}</b>
                  {Object.keys(values).length === 0 && <p className="muted">Chưa có dữ liệu.</p>}
                  {Object.entries(values).map(([status, count], index) => (
                    <div className="status-bar" key={status}>
                      <span>{status.replaceAll('_', ' ')}</span>
                      <span className="status-bar__track">
                        <span className="status-bar__fill" style={{ width: `${total ? (count / total) * 100 : 0}%`, background: STATUS_COLORS[index % STATUS_COLORS.length] }} />
                      </span>
                      <b>{count}</b>
                    </div>
                  ))}
                </div>
              );
            })}
          </div>
        </article>

        <article className="data-card">
          <div className="section-heading"><div><p className="eyebrow">CHI TIẾT</p><h3>Cảnh báo theo loại</h3></div></div>
          {alertTypes.length ? (
            <ul className="alert-list">
              {alertTypes.map((item) => (
                <li key={item.name}>
                  <span className="severity"><AlertTriangle size={16} /></span>
                  <div><b>{item.name.replaceAll('_', ' ')}</b><small>{item.value} sự kiện</small></div>
                </li>
              ))}
            </ul>
          ) : <p className="muted">Chưa có cảnh báo nào.</p>}
        </article>
      </section>
    </>
  );
}
