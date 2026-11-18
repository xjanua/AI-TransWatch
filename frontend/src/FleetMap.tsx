import { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { LiveVehicle } from './api';

/** Vị trí mặc định: trung tâm TP. Hồ Chí Minh (khu vực dữ liệu demo). */
const DEFAULT_CENTER: L.LatLngExpression = [10.8231, 106.6297];
const DEFAULT_ZOOM = 11;

function vehicleIcon(label: string, moving: boolean) {
  return L.divIcon({
    className: 'fleet-marker',
    html: `<span class="fleet-marker__dot ${moving ? 'is-moving' : 'is-idle'}"></span><span class="fleet-marker__label">${label}</span>`,
    iconSize: [12, 12],
    iconAnchor: [6, 6],
  });
}

type Props = { vehicles: LiveVehicle[] };

/**
 * Bản đồ giám sát đội xe dùng Leaflet + OpenStreetMap.
 *
 * Markers được vẽ lại mỗi khi dữ liệu thay đổi nhưng bản đồ (tile layer, khung nhìn) chỉ
 * được khởi tạo một lần để không nhấp nháy khi dashboard tự động làm mới.
 */
export function FleetMap({ vehicles }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<L.Map | null>(null);
  const layerRef = useRef<L.LayerGroup | null>(null);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;
    const map = L.map(containerRef.current, { center: DEFAULT_CENTER, zoom: DEFAULT_ZOOM, scrollWheelZoom: true });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap',
    }).addTo(map);
    layerRef.current = L.layerGroup().addTo(map);
    mapRef.current = map;
    return () => {
      map.remove();
      mapRef.current = null;
      layerRef.current = null;
    };
  }, []);

  useEffect(() => {
    const layer = layerRef.current;
    if (!layer) return;
    layer.clearLayers();
    const points: L.LatLngExpression[] = [];
    for (const vehicle of vehicles) {
      if (vehicle.latitude == null || vehicle.longitude == null) continue;
      const position: L.LatLngExpression = [Number(vehicle.latitude), Number(vehicle.longitude)];
      points.push(position);
      const moving = Number(vehicle.speedKmh ?? 0) > 1;
      L.marker(position, { icon: vehicleIcon(vehicle.vehiclePlate ?? vehicle.tripCode, moving) })
        .bindPopup(
          `<b>${vehicle.vehiclePlate ?? 'Chưa rõ xe'}</b><br/>` +
            `Chuyến: ${vehicle.tripCode}<br/>` +
            `Tài xế: ${vehicle.driverName ?? '—'}<br/>` +
            `Tốc độ: ${vehicle.speedKmh != null ? Number(vehicle.speedKmh).toFixed(0) : '—'} km/h<br/>` +
            `Cập nhật: ${vehicle.recordedAt ? new Date(vehicle.recordedAt).toLocaleString('vi-VN') : 'chưa có GPS'}`,
        )
        .addTo(layer);
    }
    if (points.length && mapRef.current) {
      mapRef.current.fitBounds(L.latLngBounds(points).pad(0.25), { maxZoom: 13 });
    }
  }, [vehicles]);

  return <div className="fleet-map" ref={containerRef} role="img" aria-label="Bản đồ vị trí đội xe" />;
}
