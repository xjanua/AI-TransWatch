"""Tiện ích ML tái lập cho AI-TransWatch.

Dữ liệu là tổng hợp có seed cố định, chỉ phục vụ demo/đồ án. Khi có GPS thật,
hãy thay generator bằng pipeline làm sạch dữ liệu lịch sử và train lại model.
"""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest, RandomForestClassifier, RandomForestRegressor

FEATURE_COLUMNS = [
    "speed_kmh",
    "route_deviation_meters",
    "stopped_minutes",
    "remaining_distance_km",
    "hour_of_day",
    "load_percent",
    "historical_avg_speed_kmh",
]
MODEL_VERSION = "synthetic-rf-iforest-1.0"
ARTIFACT_PATH = Path(__file__).resolve().parent / "models" / "telemetry_models.joblib"


@dataclass(frozen=True)
class AnalysisResult:
    eta_minutes: float
    late_probability: float
    risk_level: str
    anomaly_score: float
    model_version: str
    using_baseline: bool
    explanations: list[str]


def generate_synthetic_telemetry(samples: int = 4000, seed: int = 42) -> pd.DataFrame:
    """Sinh dữ liệu telemetry có quy luật vận hành rõ ràng, có thể tái lập."""
    if samples < 100:
        raise ValueError("samples phải lớn hơn hoặc bằng 100")
    rng = np.random.default_rng(seed)
    speed = np.clip(rng.normal(48, 16, samples), 2, 110)
    deviation = np.clip(rng.gamma(1.7, 35, samples), 0, 1200)
    stop = np.clip(rng.exponential(5, samples), 0, 90)
    distance = rng.uniform(1, 180, samples)
    hour = rng.integers(0, 24, samples)
    load = rng.uniform(15, 100, samples)
    historical_speed = np.clip(rng.normal(52, 10, samples), 15, 85)
    rush_hour = np.isin(hour, [7, 8, 9, 16, 17, 18]).astype(float)

    effective_speed = np.maximum(12, speed * (1 - 0.18 * rush_hour) - deviation / 180 - stop / 8)
    eta = distance / effective_speed * 60 + stop * 0.62 + deviation / 85 + load / 42
    eta += rng.normal(0, 4, samples)
    eta = np.clip(eta, 2, None)
    late_signal = (eta > distance / np.maximum(historical_speed, 15) * 60 * 1.25) | (deviation > 300) | (stop > 22)
    late_probability = np.clip(0.06 + 0.38 * late_signal + deviation / 2200 + stop / 180 + rush_hour * 0.12 + rng.normal(0, 0.07, samples), 0, 1)

    return pd.DataFrame({
        "speed_kmh": speed,
        "route_deviation_meters": deviation,
        "stopped_minutes": stop,
        "remaining_distance_km": distance,
        "hour_of_day": hour,
        "load_percent": load,
        "historical_avg_speed_kmh": historical_speed,
        "eta_minutes": eta,
        "is_late": (late_probability >= 0.5).astype(int),
    })


def train_and_save_models(output_path: Path = ARTIFACT_PATH, samples: int = 4000, seed: int = 42) -> dict[str, Any]:
    data = generate_synthetic_telemetry(samples=samples, seed=seed)
    features = data[FEATURE_COLUMNS]
    eta_model = RandomForestRegressor(n_estimators=160, min_samples_leaf=3, random_state=seed, n_jobs=-1)
    late_model = RandomForestClassifier(n_estimators=160, min_samples_leaf=3, random_state=seed, n_jobs=-1, class_weight="balanced")
    anomaly_model = IsolationForest(n_estimators=160, contamination=0.08, random_state=seed)
    eta_model.fit(features, data["eta_minutes"])
    late_model.fit(features, data["is_late"])
    anomaly_model.fit(features)
    artifact = {"version": MODEL_VERSION, "features": FEATURE_COLUMNS, "eta_model": eta_model, "late_model": late_model, "anomaly_model": anomaly_model}
    output_path.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(artifact, output_path)
    return artifact


def load_models(path: Path = ARTIFACT_PATH) -> dict[str, Any] | None:
    return joblib.load(path) if path.exists() else None


def _risk_level(probability: float, anomaly_score: float) -> str:
    combined = probability * 0.75 + anomaly_score * 0.25
    return "HIGH" if combined >= 0.65 else "MEDIUM" if combined >= 0.32 else "LOW"


def _explain(values: dict[str, float], late_probability: float, anomaly_score: float, eta_minutes: float) -> list[str]:
    reasons: list[str] = [f"ETA dự kiến còn khoảng {eta_minutes:.0f} phút."]
    if values["route_deviation_meters"] >= 200:
        reasons.append(f"Xe lệch tuyến {values['route_deviation_meters']:.0f} m, làm tăng nguy cơ chậm trễ.")
    if values["stopped_minutes"] >= 15:
        reasons.append(f"Xe đã dừng {values['stopped_minutes']:.0f} phút, cần kiểm tra nguyên nhân.")
    if values["speed_kmh"] > 80:
        reasons.append(f"Tốc độ {values['speed_kmh']:.0f} km/h vượt ngưỡng giám sát 80 km/h.")
    if anomaly_score >= 0.65:
        reasons.append("Mẫu telemetry khác biệt đáng kể so với hành trình vận hành thông thường.")
    if late_probability >= 0.6:
        reasons.append(f"Xác suất giao trễ là {late_probability:.0%}; điều phối viên nên chủ động xử lý.")
    if len(reasons) == 1:
        reasons.append("Các chỉ số hiện tại nằm trong biên độ vận hành an toàn.")
    return reasons


def analyze(values: dict[str, float], artifact: dict[str, Any] | None = None) -> AnalysisResult:
    artifact = artifact if artifact is not None else load_models()
    frame = pd.DataFrame([{key: values[key] for key in FEATURE_COLUMNS}])
    if artifact is None:
        eta = max(2.0, values["remaining_distance_km"] / max(values["speed_kmh"], 18) * 60 + values["stopped_minutes"] + values["route_deviation_meters"] / 100)
        late = min(1.0, 0.05 + values["route_deviation_meters"] / 800 + values["stopped_minutes"] / 50 + max(values["historical_avg_speed_kmh"] - values["speed_kmh"], 0) / 100)
        anomaly = min(1.0, values["route_deviation_meters"] / 900 + values["stopped_minutes"] / 70 + max(values["speed_kmh"] - 80, 0) / 50)
        version, baseline = "baseline-transparent-1.0", True
    else:
        eta = float(artifact["eta_model"].predict(frame)[0])
        late = float(artifact["late_model"].predict_proba(frame)[0][1])
        raw_score = float(artifact["anomaly_model"].decision_function(frame)[0])
        anomaly = float(np.clip(0.5 - raw_score * 4.5, 0, 1))
        version, baseline = str(artifact["version"]), False
    risk = _risk_level(late, anomaly)
    return AnalysisResult(round(eta, 2), round(late, 4), risk, round(anomaly, 4), version, baseline, _explain(values, late, anomaly, eta))
