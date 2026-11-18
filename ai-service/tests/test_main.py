from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_is_up():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "UP"


def test_risk_prediction_returns_high_risk_for_anomalous_telemetry():
    response = client.post("/v1/predictions/risk", json={
        "speed_kmh": 95,
        "route_deviation_meters": 900,
        "stopped_minutes": 25,
        "remaining_distance_km": 14,
    })
    assert response.status_code == 200
    payload = response.json()
    assert payload["risk_level"] == "HIGH"
    assert payload["risk_score"] >= 70


def test_analyze_returns_valid_prediction():
    response = client.post("/v1/predictions/analyze", json={
        "speed_kmh": 25,
        "route_deviation_meters": 500,
        "stopped_minutes": 30,
        "remaining_distance_km": 35,
        "hour_of_day": 8,
        "load_percent": 90,
        "historical_avg_speed_kmh": 55,
    })
    assert response.status_code == 200
    payload = response.json()
    # Có thể dùng baseline khi artifact chưa được train, hoặc model đã train trong app/models.
    assert isinstance(payload["using_baseline"], bool)
    assert payload["risk_level"] in {"LOW", "MEDIUM", "HIGH"}
    assert payload["eta_minutes"] > 0
    assert payload["late_probability"] >= 0
    assert payload["anomaly_score"] >= 0
    assert any("lệch tuyến" in reason for reason in payload["explanations"])
