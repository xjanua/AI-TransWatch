FROM python:3.12-slim
WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app
COPY tests ./tests

# Huấn luyện model ngay khi build để container chạy được endpoint phân tích đầy đủ.
RUN python -c "from app.ml_workflow import train_and_save_models; train_and_save_models(); print('Đã huấn luyện model AI')"

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
