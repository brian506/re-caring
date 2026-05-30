FROM python:3.12-slim

ENV TZ=Asia/Seoul \
    PYTHONUNBUFFERED=1

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY rule_base.py .

RUN useradd -m -u 1000 worker && chown -R worker:worker /app
USER worker

CMD ["python", "-u", "rule_base.py"]
