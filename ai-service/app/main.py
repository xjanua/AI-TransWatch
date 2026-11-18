from datetime import datetime, timezone
import logging

from fastapi import FastAPI, Request
from pydantic import AliasChoices, BaseModel, ConfigDict, Field

from app.ml_workflow import analyze

logger = logging.getLogger("ai-transwatch.telemetry")

app = FastAPI(
    title="AI-TransWatch AI Service",
    version="0.2.0",
    description="Dịch vụ dự báo ETA, nguy cơ giao trễ và phát hiện bất thường hành trình.",
)


@app.middleware("http")
async def log_failed_analyze_payload(request: Request, call_next):
    """Khi /analyze trả lỗi 4xx/5xx, ghi lại phần thân yêu cầu để chẩn đoán tích hợp."""
    if request.url.path == "/v1/predictions/analyze" and request.method == "POST":
        raw = await request.body()
        response = await call_next(request)
        if response.status_code >= 400:
            logger.warning("analyze %d lỗi %d, payload (%d bytes): %s", response.status_code, response.status_code,
                           len(raw), raw[:500].decode("utf-8", "replace"))
        return response
    return await call_next(request)


class TripTelemetry(BaseModel):
    """Telemetry đầu vào.

    Chấp nhận cả snake_case (chuẩn Python) và camelCase để tương thích với
    client Java/Jackson khác phiên bản mà không phá vỡ hợp đồng hiện có.
    """

    model_config = ConfigDict(populate_by_name=True, extra="ignore")

    speed_kmh: float = Field(ge=0, le=180, validation_alias=AliasChoices("speed_kmh", "speedKmh"))
    route_deviation_meters: float = Field(ge=0, le=50_000, validation_alias=AliasChoices("route_deviation_meters", "routeDeviationMeters"))
    stopped_minutes: float = Field(ge=0, le=1_440, validation_alias=AliasChoices("stopped_minutes", "stoppedMinutes"))
    remaining_distance_km: float = Field(ge=0, le=5_000, validation_alias=AliasChoices("remaining_distance_km", "remainingDistanceKm"))
    hour_of_day: int = Field(default=12, ge=0, le=23, validation_alias=AliasChoices("hour_of_day", "hourOfDay"))
    load_percent: float = Field(default=50, ge=0, le=100, validation_alias=AliasChoices("load_percent", "loadPercent"))
    historical_avg_speed_kmh: float = Field(default=50, gt=0, le=180, validation_alias=AliasChoices("historical_avg_speed_kmh", "historicalAvgSpeedKmh"))


@app.get("/health")
def health() -> dict:
    return {"service": "ai-service", "status": "UP", "timestamp": datetime.now(timezone.utc).isoformat()}


@app.post("/v1/predictions/risk")
def estimate_risk(telemetry: TripTelemetry) -> dict:
    """Endpoint tương thích ngược trả risk score minh bạch."""
    score = min(100.0, telemetry.route_deviation_meters / 25 + telemetry.stopped_minutes * 1.5 + max(0, telemetry.speed_kmh - 60) * 1.2)
    level = "HIGH" if score >= 70 else "MEDIUM" if score >= 35 else "LOW"
    return {"risk_score": round(score, 2), "risk_level": level, "model_version": "baseline-0.1"}


@app.post("/v1/predictions/analyze")
def analyze_trip(telemetry: TripTelemetry) -> dict:
    """Trả ETA, xác suất trễ và anomaly score từ artifact hoặc baseline an toàn."""
    result = analyze(telemetry.model_dump())
    return {
        "eta_minutes": result.eta_minutes,
        "late_probability": result.late_probability,
        "risk_level": result.risk_level,
        "anomaly_score": result.anomaly_score,
        "model_version": result.model_version,
        "using_baseline": result.using_baseline,
        "explanations": result.explanations,
    }
