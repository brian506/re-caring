#!/bin/sh
  set -e

  DOMAIN="${DOMAIN:-re-caring.com}"
  EMAIL="${EMAIL:-choiyngmin506@gmail.com}"
  AWS_REGION="${AWS_REGION:-ap-northeast-2}"

  echo "[Certbot : 시작]: domain=${DOMAIN}"

  # 최초 발급 vs 갱신 분기
  if [ -f "/etc/letsencrypt/live/${DOMAIN}/fullchain.pem" ]; then
    echo "[Certbot : 갱신 시도]"
    certbot renew --non-interactive
  else
    echo "[Certbot : 최초 발급 시도]"
    certbot certonly \
      --webroot \
      --webroot-path /var/www/certbot \
      -d "${DOMAIN}" \
      --email "${EMAIL}" \
      --agree-tos \
      --non-interactive
  fi

  echo "[Certbot : 인증서 처리 완료]"

  # Nginx force-new-deployment (갱신된 인증서 로드)
  echo "[ECS : Nginx 재배포 시작]: cluster=${ECS_CLUSTER} service=${ECS_SERVICE}"
  aws ecs update-service \
    --cluster "${ECS_CLUSTER}" \
    --service "${ECS_SERVICE}" \
    --force-new-deployment \
    --region "${AWS_REGION}" \
    --no-cli-pager

  echo "[ECS : Nginx 재배포 요청 완료]"