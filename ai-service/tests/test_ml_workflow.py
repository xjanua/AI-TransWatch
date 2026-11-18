from app.ml_workflow import analyze, generate_synthetic_telemetry, load_models, train_and_save_models


def test_generator_is_reproducible():
    first = generate_synthetic_telemetry(samples=120, seed=9)
    second = generate_synthetic_telemetry(samples=120, seed=9)
    assert first.equals(second)
    assert {"eta_minutes", "is_late", "speed_kmh"}.issubset(first.columns)


def test_training_artifact_is_used(tmp_path):
    artifact_path = tmp_path / "models.joblib"
    train_and_save_models(output_path=artifact_path, samples=250, seed=12)
    artifact = load_models(artifact_path)
    result = analyze({
        "speed_kmh": 45.0, "route_deviation_meters": 30.0, "stopped_minutes": 2.0,
        "remaining_distance_km": 20.0, "hour_of_day": 11.0, "load_percent": 40.0,
        "historical_avg_speed_kmh": 50.0,
    }, artifact)
    assert result.using_baseline is False
    assert result.model_version == artifact["version"]
    assert result.eta_minutes > 0
    assert 0 <= result.late_probability <= 1
    assert 0 <= result.anomaly_score <= 1
