package me.xjanua.aitranswatch.dto.demo;

import jakarta.validation.constraints.NotNull;

/** Kịch bản GPS xác định phục vụ demo; không phải nguồn telemetry sản xuất. */
public record DemoSimulationRequest(@NotNull Scenario scenario) {
    public enum Scenario {
        NORMAL,
        SPEEDING,
        ABNORMAL_STOP,
        /** Xe đi xa lộ trình dự kiến để kích hoạt cảnh báo ROUTE_DEVIATION. */
        ROUTE_DEVIATION,
        /** Điểm telemetry lùi 4,5 giờ để mô phỏng ca lái liên tục quá ngưỡng. */
        CONTINUOUS_DRIVING
    }
}
