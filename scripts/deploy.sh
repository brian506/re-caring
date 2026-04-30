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
if docker exec nginx nginx -s reload 2>&1; then
  echo "  nginx reload 성공"
else
  echo "  [WARN] nginx reload 실패 — SSL 인증서 미발급 상태일 수 있음. certbot 실행 후 재시도 필요."
fi

echo "=== 완료 ==="
