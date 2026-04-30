#!/bin/bash
set -euo pipefail

DEPLOY_DIR=/home/ubuntu/recaring

echo "=== 설정 파일 존재 확인 ==="
for path in "$DEPLOY_DIR/nginx/conf.d" \
            "$DEPLOY_DIR/data/certbot/conf" \
            "$DEPLOY_DIR/data/certbot/www" \
            "$DEPLOY_DIR/monitoring/grafana/provisioning"; do
  if [ ! -e "$path" ]; then
    echo "누락된 경로: $path"
    exit 1
  fi
  echo "  OK: $path"
done

echo "=== nginx 설정 리로드 ==="
docker exec nginx nginx -s reload

echo "=== 완료 ==="
