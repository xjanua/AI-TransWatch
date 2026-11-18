package me.xjanua.aitranswatch.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.xjanua.aitranswatch.enums.RoutePointType;
import me.xjanua.aitranswatch.model.TripRoutePoint;

/** Kiểm thử đơn vị tính khoảng cách phục vụ phát hiện lệch tuyến. */
class GeoUtilsTest {

    private static TripRoutePoint point(double latitude, double longitude) {
        return new TripRoutePoint(null, 1, RoutePointType.WAYPOINT,
                BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude), null, null);
    }

    @Test
    @DisplayName("Khoảng cách giữa hai điểm cùng vị trí bằng 0")
    void samePointHasZeroDistance() {
        assertThat(GeoUtils.distanceMeters(10.85, 106.79, 10.85, 106.79)).isZero();
    }

    @Test
    @DisplayName("Khoảng cách 0.01 độ vĩ tuyến xấp xỉ 1.1 km")
    void latitudeDeltaIsAboutOneKilometer() {
        double distance = GeoUtils.distanceMeters(10.85, 106.79, 10.86, 106.79);

        assertThat(distance).isBetween(1_050d, 1_180d);
    }

    @Test
    @DisplayName("Điểm nằm trên đoạn lộ trình có khoảng cách gần 0")
    void pointOnSegmentHasNearZeroDistance() {
        List<TripRoutePoint> route = List.of(point(10.80, 106.80), point(10.90, 106.80));

        Double distance = GeoUtils.distanceToRouteMeters(route, 10.85, 106.80);

        assertThat(distance).isNotNull();
        assertThat(distance).isLessThan(50d);
    }

    @Test
    @DisplayName("Điểm lệch 0.03 độ khỏi lộ trình bị coi là lệch tuyến")
    void deviatedPointIsFarFromRoute() {
        List<TripRoutePoint> route = List.of(point(10.80, 106.80), point(10.90, 106.80));

        Double distance = GeoUtils.distanceToRouteMeters(route, 10.85, 106.83);

        assertThat(distance).isNotNull();
        assertThat(distance).isGreaterThan(3_000d);
    }

    @Test
    @DisplayName("Lộ trình rỗng trả về null để bỏ qua luật lệch tuyến")
    void emptyRouteReturnsNull() {
        assertThat(GeoUtils.distanceToRouteMeters(new ArrayList<>(), 10.85, 106.80)).isNull();
        assertThat(GeoUtils.distanceToRouteMeters(null, 10.85, 106.80)).isNull();
    }

    @Test
    @DisplayName("Khoảng cách tới đoạn thẳng tính theo hình chiếu vuông góc")
    void projectsOntoSegment() {
        double beyondStart = GeoUtils.distanceToSegmentMeters(10.79, 106.80, 10.80, 106.80, 10.90, 106.80);
        double middle = GeoUtils.distanceToSegmentMeters(10.85, 106.801, 10.80, 106.80, 10.90, 106.80);

        assertThat(beyondStart).isGreaterThan(1_000d);
        assertThat(middle).isLessThan(200d);
    }
}
