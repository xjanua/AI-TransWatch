package me.xjanua.aitranswatch.util;

import java.util.List;

import me.xjanua.aitranswatch.model.TripRoutePoint;

/** Tính toán khoảng cách địa lý phục vụ phát hiện lệch tuyến. */
public final class GeoUtils {
    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    private GeoUtils() {
    }

    /** Khoảng cách đường chim bay giữa hai điểm (mét) theo công thức haversine. */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1d, Math.sqrt(a)));
    }

    /**
     * Khoảng cách ngắn nhất từ một điểm tới lộ trình dự kiến (đường gấp khúc qua các điểm).
     *
     * @return khoảng cách theo mét, hoặc {@code null} nếu lộ trình chưa có điểm nào
     */
    public static Double distanceToRouteMeters(List<TripRoutePoint> route, double latitude, double longitude) {
        if (route == null || route.isEmpty()) {
            return null;
        }
        if (route.size() == 1) {
            TripRoutePoint only = route.get(0);
            return distanceMeters(latitude, longitude, only.getLatitude().doubleValue(), only.getLongitude().doubleValue());
        }
        double best = Double.MAX_VALUE;
        for (int index = 0; index < route.size() - 1; index++) {
            TripRoutePoint from = route.get(index);
            TripRoutePoint to = route.get(index + 1);
            best = Math.min(best, distanceToSegmentMeters(latitude, longitude,
                    from.getLatitude().doubleValue(), from.getLongitude().doubleValue(),
                    to.getLatitude().doubleValue(), to.getLongitude().doubleValue()));
        }
        return best;
    }

    /**
     * Khoảng cách từ điểm tới đoạn thẳng nối hai điểm, chiếu theo mặt phẳng cục bộ
     * (đủ chính xác cho khoảng cách vài trăm mét tới vài kilomet của một chuyến xe).
     */
    public static double distanceToSegmentMeters(double lat, double lon,
            double lat1, double lon1, double lat2, double lon2) {
        double referenceLatRadians = Math.toRadians((lat1 + lat2) / 2);
        double metersPerDegreeLat = 111_132d;
        double metersPerDegreeLon = 111_320d * Math.cos(referenceLatRadians);

        double px = (lon - lon1) * metersPerDegreeLon;
        double py = (lat - lat1) * metersPerDegreeLat;
        double dx = (lon2 - lon1) * metersPerDegreeLon;
        double dy = (lat2 - lat1) * metersPerDegreeLat;

        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0d) {
            return Math.hypot(px, py);
        }
        double t = Math.max(0d, Math.min(1d, (px * dx + py * dy) / lengthSquared));
        return Math.hypot(px - t * dx, py - t * dy);
    }
}
