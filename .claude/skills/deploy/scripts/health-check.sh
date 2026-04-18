#!/bin/bash
# 배포 서버의 Actuator 헬스체크를 수행한다
# Usage: bash health-check.sh

HEALTH_URL="https://re-caring.duckdns.org/actuator/health"
MAX_RETRY=5
INTERVAL=15

echo "=== Health check: $HEALTH_URL ==="

for i in $(seq 1 $MAX_RETRY); do
  RESPONSE=$(curl -s --max-time 10 "$HEALTH_URL")
  STATUS=$(echo "$RESPONSE" | grep -o '"status":"[^"]*"' | head -1)

  echo "[$i/$MAX_RETRY] Response: $STATUS"

  if echo "$STATUS" | grep -q '"UP"'; then
    echo "=== Server is UP ==="
    exit 0
  fi

  echo "Retrying in ${INTERVAL}s..."
  sleep "$INTERVAL"
done

echo "=== Health check FAILED after $MAX_RETRY attempts ==="
echo "Last response: $RESPONSE"
exit 1
