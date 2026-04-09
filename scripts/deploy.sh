#!/bin/bash
set -e

DEPLOY_DIR=/home/ubuntu/recaring
COMPOSE_FILE=$DEPLOY_DIR/docker-compose-dev.yml
SERVICE_URL_FILE=$DEPLOY_DIR/nginx/conf.d/service-url.inc
HEALTH_CHECK_RETRIES=12
HEALTH_CHECK_INTERVAL=5

# 다른 인프라 컨테이너 Up
docker compose -f $COMPOSE_FILE up -d redis nginx alloy prometheus loki grafana certbot

echo "=== Blue-Green 배포 시작 ==="

# 현재 running 중인 컨테이너 확인
BLUE_RUNNING=$(docker ps -q -f name=recaring-blue -f status=running)
GREEN_RUNNING=$(docker ps -q -f name=recaring-green -f status=running)

if [ -n "$BLUE_RUNNING" ]; then
    CURRENT="blue"
    NEXT="green"
    NEXT_PORT=8081
    echo "현재 Active: blue → 다음 배포 대상: green (포트 $NEXT_PORT)"
else
    CURRENT="green"
    NEXT="blue"
    NEXT_PORT=8080
    echo "현재 Active: green (또는 최초 배포) → 다음 배포 대상: blue (포트 $NEXT_PORT)"
fi

# 새 이미지 pull
echo ">>> 새 이미지 pull 중..."
docker compose -f $COMPOSE_FILE pull spring-$NEXT

# 비활성 컨테이너 시작
echo ">>> spring-$NEXT 컨테이너 시작..."
docker compose -f $COMPOSE_FILE up -d --force-recreate --no-deps spring-$NEXT
# 헬스체크: 포트가 응답할 때까지 대기
echo ">>> 헬스체크 중... (최대 ${HEALTH_CHECK_RETRIES}회 × ${HEALTH_CHECK_INTERVAL}초)"
for i in $(seq 1 $HEALTH_CHECK_RETRIES); do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$NEXT_PORT/actuator/health || echo "000")
    if [ "$HTTP_CODE" == "200" ]; then
        echo "헬스체크 성공 (HTTP 200)"
        break
    fi
    if [ "$i" -eq "$HEALTH_CHECK_RETRIES" ]; then
        echo "헬스체크 실패 - 배포 롤백"
        docker compose -f $COMPOSE_FILE stop spring-$NEXT
        exit 1
    fi
    echo "  대기 중... ($i/$HEALTH_CHECK_RETRIES)"
    sleep $HEALTH_CHECK_INTERVAL
done

# nginx upstream을 새 컨테이너로 교체
echo ">>> nginx upstream → spring-$NEXT:8080 으로 전환..."
echo "set \$service_url http://spring-$NEXT:8080;" > $SERVICE_URL_FILE
docker exec nginx nginx -s reload
echo "nginx 전환 완료"

# 이전 컨테이너 중지 (최초 배포 시에는 건너뜀)
if [ -n "$BLUE_RUNNING" ] || [ -n "$GREEN_RUNNING" ]; then
    echo ">>> spring-$CURRENT 컨테이너 중지..."
    docker compose -f $COMPOSE_FILE stop spring-$CURRENT
fi

echo "=== 배포 완료: spring-$NEXT 이 active ==="
