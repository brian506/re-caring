# re-caring 전체 아키텍처

> 작성일: 2026-04-19
> 대상 규모: 사용자 100명 (MVP)

---

## 인프라 구성 요약

| 항목 | 선택 | 이유 |
|------|------|------|
| **런치 타입** | ECS EC2 Launch Type | 24시간 상시 실행 → Fargate보다 EC2 고정 비용이 유리 |
| **EC2 스펙** | t3.medium (4GB RAM) | 전체 컨테이너 합산 ~2.5GB, 여유 1GB 이상 |
| **SSL** | nginx + certbot | ALB($16/월) 대신 Let's Encrypt 무료 인증서 자동 갱신 |
| **GPS 파이프라인** | SQS 직접 발행 | 단일 Consumer(LLM)이므로 SNS fan-out 불필요 |
| **Redis 영속성** | ECS + EFS AOF | ElastiCache($24/월) 대신 EFS($3/월)로 데이터 유실 방지 |
| **모니터링** | Prometheus + Grafana + CloudWatch Logs | 메트릭: Prometheus 직접 스크래핑, 로그: awslogs → CloudWatch |

---

## 전체 아키텍처 다이어그램

```
[보호대상자 앱]              [보호자 앱]
      │ GPS 전송 (1분마다)         │ 위치 조회 / SSE 연결
      │ HTTPS                      │ HTTPS
      └────────────────────────────┘
                     │
      ┌──────────────▼──────────────────────────────────────────┐
      │  EC2 t3.medium  (ECS EC2 Launch Type)                   │
      │                                                         │
      │  ┌──────────────────────────────────────────────────┐  │
      │  │  ECS Service: nginx + certbot                     │  │
      │  │  - 포트 80  → HTTPS 리다이렉트                    │  │
      │  │  - 포트 443 → Let's Encrypt SSL (자동 갱신)       │  │
      │  │  - upstream → spring-app:8080                     │  │
      │  └───────────────────────┬──────────────────────────┘  │
      │                          │                              │
      │  ┌───────────────────────▼──────────────────────────┐  │
      │  │  ECS Service: spring-app                          │  │
      │  │                                                   │  │
      │  │  GPS 수신 시 4가지 동작:                          │  │
      │  │  ① DB INSERT   → GPS 이력 저장 (경로 조회용)     │  │
      │  │  ② Redis SET   → gps:latest:{key}, TTL 5분       │  │
      │  │  ③ SSE push    → 연결된 보호자에게 실시간 전송    │  │
      │  │  ④ SQS send    → gps-llm-queue                   │  │
      │  │                                                   │  │
      │  │  LLM 이상감지 수신 시:                            │  │
      │  │  ⑤ HTTP POST 수신 → Push 알림 발송               │  │
      │  └───────┬──────────────────────┬───────────────────┘  │
      │          │                      │                       │
      │  ┌───────▼──────┐   ┌───────────▼──────────────────┐  │
      │  │  ECS Service  │   │  RDS: PostgreSQL 16           │  │
      │  │  redis        │   │  - GPS 이력                   │  │
      │  │  (EFS + AOF)  │   │  - 회원 정보 전체             │  │
      │  │               │   │  - db.t3.micro, gp3 20GB     │  │
      │  │  refresh:{}   │   └──────────────────────────────┘  │
      │  │  sms:{}       │                                      │
      │  │  gps:latest:{}│                                      │
      │  └───────────────┘                                      │
      │                                                         │
      │  ┌──────────────────────────────────────────────────┐  │
      │  │  ECS Service: llm-container                       │  │
      │  │  - SQS gps-llm-queue 폴링                        │  │
      │  │  - 이상 경로 패턴 감지                            │  │
      │  │  - 감지 시 → HTTP POST → spring-app              │  │
      │  └──────────────────────────────────────────────────┘  │
      │                                                         │
      │  ┌──────────────────────────────────────────────────┐  │
      │  │  ECS Service: monitoring                          │  │
      │  │                                                   │  │
      │  │  [Prometheus] ← /actuator/prometheus 스크래핑    │  │
      │  │    메트릭 저장 (Docker named volume, 15일 보존)   │  │
      │  │                                                   │  │
      │  │  [Grafana :3000]                                  │  │
      │  │    ├─ 데이터소스: Prometheus (메트릭)             │  │
      │  │    └─ 데이터소스: CloudWatch Logs (로그)         │  │
      │  └──────────────────────────────────────────────────┘  │
      │                                                         │
      │  spring-app → [awslogs 드라이버] → CloudWatch Logs     │
      │                                    (/ecs/recaring)      │
      └─────────────────────────────────────────────────────────┘
                          │ SQS send
                          ▼
               ┌──────────────────────┐
               │  AWS SQS:            │
               │  gps-llm-queue       │  (LLM 분석용)
               └──────────────────────┘
                          │ 폴링
                          ↑
                 llm-container (EC2 내)
```

---

## 데이터 흐름

### 1. GPS 수신 흐름 (보호대상자 → 시스템)

```
보호대상자 앱
  → (HTTPS) nginx
  → spring-app
       ├─ PostgreSQL INSERT  (lat, lng, timestamp, memberKey)
       ├─ Redis SET          gps:latest:{memberKey} = {lat, lng, ts}  TTL 5분
       ├─ SSE push           해당 보호대상자를 구독 중인 보호자 채널로 즉시 전송
       └─ SQS send           gps-llm-queue → LLM 컨테이너
```

### 2. 보호자 위치 조회 흐름

```
보호자 앱 (지도 화면 — 실시간)
  → spring-app SSE 연결 (/location/stream/{memberKey})
  → GPS 수신마다 SSE push
  → Redis gps:latest:{memberKey} 초기값 로드 (Cache Miss 시 DB fallback)

보호자 앱 (경로 히스토리 조회 — 날짜별)
  → spring-app REST API
  → PostgreSQL 날짜 범위 쿼리 (Redis 불필요)
```

### 3. LLM 이상감지 흐름

```
SQS gps-llm-queue
  → llm-container 폴링 (30초 간격)
  → GPS 좌표 패턴 분석
  → 이상 감지 시: HTTP POST → spring-app /internal/anomaly
  → spring-app → FCM Push 알림 발송
```

### 4. 모니터링 흐름

```
메트릭:
  Spring /actuator/prometheus
    → Prometheus 직접 스크래핑 (15초 간격)
    → Docker named volume 저장 (15일 보존)
    → Grafana 시각화

로그:
  spring-app ECS Task → awslogs 드라이버
    → CloudWatch Logs (/ecs/recaring 로그 그룹)
    → Grafana CloudWatch 데이터소스로 조회
```

---

## 컴포넌트별 ECS 설정

| ECS Service | 이미지 | 메모리 한도 | 포트 | 볼륨 |
|------------|--------|------------|------|------|
| nginx | nginx:latest | 128MB | 80, 443 (호스트 바인딩) | EFS(certbot certs), nginx conf |
| certbot | certbot/certbot | 64MB | — | EFS(certbot certs) |
| spring-app | brianchoi506/re-caring:latest | 600MB | 8080 | — (로그: awslogs → CloudWatch) |
| redis | redis:7-alpine | 128MB | 6379 | EFS(/data, AOF) |
| llm-container | (별도 이미지) | 512MB+ | — | — |
| prometheus | prom/prometheus | 256MB | 9090 | Docker named volume |
| grafana | grafana/grafana:latest | 192MB | 3000 | Docker named volume |
| **합계 (EC2)** | | **~1.9GB** | | |

**PostgreSQL → RDS (EC2 외부, AWS 관리형)**

| RDS | 엔진 | 스펙 | 스토리지 | 비고 |
|-----|------|------|---------|------|
| recaring-db | PostgreSQL 16 | db.t3.micro | gp3 20GB | 자동 백업 7일, 포인트인타임 복구 |

> PostgreSQL을 ECS + EFS로 운영하면 NFS 위에 DB 데이터 디렉토리가 올라가 fsync 동작 차이와 파일 락 이슈로 데이터 손상 위험이 있음. 핵심 비즈니스 데이터(GPS 이력, 회원 정보)는 RDS로 분리.

> **Spring JVM 메모리 제한 필수**: `JAVA_OPTS="-Xms256m -Xmx512m"`
> t3.medium 4GB → OS + ECS Agent ~400MB 제외 시 가용 3.6GB, 여유 약 1.1GB

---

## Redis 데이터 구조

```
Redis (EFS AOF 활성화: --appendonly yes --appendfsync everysec)
  ├── refresh:{memberKey}        RefreshToken          TTL: 7일
  ├── sms:{phone}                SMS 인증코드          TTL: 3분
  └── gps:latest:{memberKey}     GPS 최신 위치         TTL: 5분
                                 { lat, lng, timestamp }
```

Task 재시작 시 EFS의 AOF 파일로 자동 복구. 다운타임 30~60초, 데이터 유실 없음.

---

## 장애 격리

| 장애 대상 | 영향 범위 | 복구 방식 |
|---------|---------|---------|
| Spring 크래시 | Spring만 30~60초 불가 | ECS 자동 재시작 |
| Redis 크래시 | 로그인/캐시 일시 불가 | ECS 자동 재시작 + EFS AOF 복구 |
| RDS 크래시 | GPS 저장 일시 실패 | RDS 자동 복구 (Multi-AZ 미적용 시 수분 소요) |
| LLM 크래시 | 이상감지 알림만 불가 | ECS 자동 재시작 |
| Monitoring 크래시 | Grafana 화면만 불가 | ECS 자동 재시작 |
| SQS 다운 | LLM 분석 불가 | AWS 관리형 Multi-AZ 자동 복구 |
| EC2 크래시 | 전체 영향 | EC2 Auto Recovery (MVP 수준 감수) |

---

## 배포 파이프라인

```
GitHub Actions
  → ECR push
  → aws ecs update-service --force-new-deployment
  → 신규 Task 기동 → Cloud Map에 등록 → 헬스체크 통과
  → nginx가 Cloud Map DNS 재확인 (TTL 10s) → 트래픽 신규 Task로 전환
  → 구 Task Cloud Map 해제 → 구 Task 종료 (무중단)
```

- nginx upstream: `resolver 169.254.169.253 valid=10s;` + Cloud Map DNS명(`spring-app.recaring.local`) 사용
- spring-app ECS 서비스: `minimum_healthy_percent=100, maximum_percent=200` (신규 기동 후 구 종료)

---

## AWS 관리형 서비스

| 서비스 | 용도 | 비고 |
|--------|------|------|
| SQS (gps-llm-queue) | LLM 비동기 파이프라인 | Standard Queue, 가시성 타임아웃 60초 |
| SSM Parameter Store | DB/JWT/API 시크릿 관리 | Standard (무료) |
| EFS | Redis AOF + certbot certs | ~$1/월 (Spring 로그는 CloudWatch awslogs) |
| CloudWatch Logs | Spring 애플리케이션 로그 (awslogs) | /ecs/recaring 로그 그룹, 무료 티어 5GB/월 |
| ECR | 컨테이너 이미지 저장 | ~$0.5/월 |

---

## 월 비용

| 항목 | 월 비용 |
|------|--------|
| EC2 t3.medium | $30 |
| RDS db.t3.micro + gp3 20GB | $15 |
| EFS (Redis AOF + certs) | ~$1 |
| SQS | ~$0.5 |
| ECR | ~$0.5 |
| CloudWatch Logs | ~$0 (MVP 무료 티어) |
| **합계** | **~$47/월** |

---

## 향후 스케일업 전환 포인트

| 시점 | 조치 |
|------|------|
| 사용자 수백 명 이상 | EC2 t3.large 업그레이드 또는 Fargate 전환 |
| PostgreSQL 부하 증가 | RDS Multi-AZ 활성화 |
| Redis 가용성 강화 필요 | ElastiCache t4g.micro + Replica 1개 |
| 보호자 동시 접속 증가 | Spring 다중 Task + Redis Pub/Sub SSE 동기화 추가 |
| LLM 처리량 증가 | LLM 전용 EC2 분리 또는 SQS Consumer 수평 확장 |
| FCM 푸시 필요 시 (복수 이벤트 트리거) | SNS 재도입 + SQS 구독 구조로 전환 |
