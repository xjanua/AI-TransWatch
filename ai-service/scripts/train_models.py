"""Chạy: python scripts/train_models.py [--samples 4000] [--seed 42]."""
import argparse
import sys
from pathlib import Path

# Cho phép chạy trực tiếp từ thư mục ai-service như hướng dẫn README.
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.ml_workflow import ARTIFACT_PATH, MODEL_VERSION, train_and_save_models


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Huấn luyện mô hình telemetry tổng hợp AI-TransWatch")
    parser.add_argument("--samples", type=int, default=4000)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()
    train_and_save_models(samples=args.samples, seed=args.seed)
    print(f"Saved artifact {MODEL_VERSION}: {ARTIFACT_PATH}")
