# re-caring 인프라 구조 설명

> 대상: 팀원 온보딩 / 구조 파악용  
> 기준: 2026-05-17 실측 (AWS CLI 직접 조회)

---

## 1. 한 줄 요약

**단일 EC2 t3.medium 위에 ECS EC2 Launch Type으로 모든 컨테이너를 운영한다.**  
ALB, Fargate 없이 Nginx + Certbot으로 HTTPS를 처리하고, 월 ~$47에 운영한다.

---

## 2. AWS 기본 정보

| 항목 | 값 |
|------|----|
| 리전 | ap-northeast-2 (서울) |
| 계정 ID | 757052694924 |
| EC2 인스턴스 | `i-0c3a7ea59344d10b0` (recaring-app-server) |
| EC2 스펙 | t3.medium · vCPU 2 · RAM 4GB |
| 퍼블릭 IP | 43.200.235.247 |
| 프라이빗 IP | 10.0.1.158 |
| EC2 상태 | Running |

---

## 3. 네트워크 구조

```
recaring-vpc (10.0.0.0/16)
│
├── Public Subnet  10.0.1.0/24  ap-northeast-2a  ← EC2 위치
├── Private Subnet 10.0.2.0/24  ap-northeast-2a  ← RDS 위치
└── Private Subnet 10.0.3.0/24  ap-northeast-2c  ← LSTM(AI) 위치
```

### 트래픽 흐름

```
인터넷
  └── IGW (igw-0c49345a05537238a)
        └── Public Subnet (10.0.1.0/24)
              └── EC2 t3.medium
                    └── Nginx (포트 80/443)
                          └── Spring App (포트 8080)
                                └── RDS (Private Subnet, 10.0.2.0/24)
```

### 보안그룹 (EC2 인바운드)

| 포트 | 프로토콜 | 소스 | 용도 |
|------|---------|------|------|
| 80 | TCP | 0.0.0.0/0 | HTTP (HTTPS 리다이렉트) |
| 443 | TCP | 0.0.0.0/0 | HTTPS |
| 8080 | TCP | VPC 내부 | Spring App (내부 통신) |

> **SSH(22)는 완전히 차단.** EC2 접근은 AWS SSM Session Manager만 사용.

---

## 4. ECS 컨테이너 구성

모든 서비스는 ECS EC2 Launch Type. 네트워크 모드가 서비스마다 다르다.

### 4-1. Bridge 모드 (Nginx · Redis · Monitoring)

EC2의 Primary ENI를 공유하는 Docker 가상 네트워크. 컨테이너끼리 컨테이너명으로 통신.

| 서비스 | 이미지 출처 | 메모리 | 포트 | 볼륨 |
|--------|-----------|--------|------|------|
| nginx | ECR (recaring-nginx) | 128MB | 80, 443 (호스트 바인딩) | EFS (인증서) |
| redis | ECR (recaring-redis) | 128MB | 6379 | EFS (AOF 영속화) |
| prometheus | prom/prometheus | 256MB | 9090 | Docker named volume |
| grafana | grafana/grafana | 192MB | 3000 | Docker named volume |
| alertmanager | prom/alertmanager | — | 9093 | — |
| node-exporter | prom/node-exporter | — | 9100 | — |
| redis-exporter | oliver006/redis_exporter | — | 9121 | — |

### 4-2. awsvpc 모드 (Spring App)

Task마다 독립 ENI 할당 → 롤링 업데이트 무중단 배포 방식

| 서비스 | 이미지 출처 | 메모리 | 포트 |
|--------|-----------|--------|------|
| spring-app | ECR (re-caring-api) | 600MB (JVM: -Xmx512m) | 8080 |

Task가 2개 뜰 때 (배포 시):
```
Task ENI 1 → 10.0.0.1:8080  (App V1, 구버전)
Task ENI 2 → 10.0.0.2:8080  (App V2, 신버전)
```

### 4-3. Fargate (Certbot — 단발성)

```
EventBridge (스케줄) → Certbot Task 기동 → 인증서 갱신 → EFS 저장 → Task 종료
```

평소에는 실행 안 함. 인증서 갱신 시에만 Fargate로 띄워서 처리.

---

## 5. 서비스 디스커버리 (Cloud Map)

Spring App은 awsvpc 모드라 IP가 Task마다 바뀜. Nginx가 항상 현재 IP를 알려면:

```
spring-app Task 기동
  → ECS가 Cloud Map에 app.local DNS 등록
  → Nginx resolver 169.254.169.253 valid=10s;
  → Nginx가 app.local 조회 → 현재 Task IP로 라우팅
```

배포 시 신규 Task가 Cloud Map에 등록되면 10초 내로 Nginx가 트래픽을 전환.  
`minimum_healthy_percent=100` 설정으로 구 Task가 살아있는 상태에서 신규 Task 기동 → 무중단.

---

## 6. 데이터 계층

### RDS (PostgreSQL)

| 항목 | 값 |
|------|----|
| 식별자 | recaring-db |
| 엔진 | PostgreSQL 17.6 |
| 스펙 | db.t4g.micro |
| 스토리지 | gp3 20GB |
| 위치 | Private Subnet ap-northeast-2c |
| Multi-AZ | ❌ (MVP 수준 감수) |
| 자동 백업 | 7일 보존, 포인트인타임 복구 |
| 접근 | EC2 SG → RDS SG 허용 (퍼블릭 노출 없음) |

저장 데이터: GPS 이력, 회원 정보, 케어 관계, 인증 정보 등 모든 핵심 비즈니스 데이터.

> PostgreSQL을 ECS + EFS로 올리지 않는 이유: NFS 위에 DB 파일을 올리면 fsync 동작 차이로 데이터 손상 위험이 있음. Redis는 AOF 재생으로 복구 가능하지만 RDBMS는 불가.

### Redis

ECS 컨테이너로 운영. EFS에 AOF 파일을 저장해 Task 재시작 시 자동 복구.

```
Redis 키 구조
├── refresh:{memberKey}      RefreshToken       TTL: 7일
├── sms:{phone}              SMS 인증코드       TTL: 3분
└── gps:latest:{memberKey}   GPS 최신 위치      TTL: 5분
                             { lat, lng, timestamp }
```

Task 재시작 → EFS AOF 재생 → 30~60초 내 복구, 데이터 유실 없음.

### EFS (공유 스토리지)

두 가지 용도로 사용:
1. **Redis AOF** — Redis Task가 마운트, 영속화
2. **SSL 인증서** — Certbot Task와 Nginx Task가 공유 마운트

---

## 7. SSL 인증서 자동갱신

ALB 없이 Let's Encrypt 인증서를 직접 운영하는 구조.

```
[갱신 주기마다]
EventBridge (스케줄러)
  → Certbot Task 기동 (Fargate, ECR: recaring-certbot)
  → Let's Encrypt로 인증서 갱신
  → EFS에 인증서 파일 저장
  → Nginx에 reload 신호
  → Nginx가 EFS에서 새 인증서 읽어 적용
  → Certbot Task 종료
```

Nginx와 Certbot이 EFS를 공유하므로 다운타임 없이 인증서 교체됨.

---

## 8. 모니터링

### 메트릭 파이프라인

```
Spring App /actuator/prometheus
  ← Prometheus 15초마다 pull 스크래핑

Redis ← redis-exporter → Prometheus (Redis 메트릭)
EC2 호스트 ← node-exporter → Prometheus (CPU/메모리/디스크)

Prometheus
  → Docker named volume 저장 (15일 보존)
  → Grafana 대시보드 시각화
  → Alertmanager → Slack/웹훅 알림
```

### 로그 파이프라인

```
Spring App ECS Task
  → awslogs 드라이버
  → CloudWatch Logs (/ecs/recaring 로그 그룹)
  → Grafana CloudWatch 데이터소스로 조회
```

> Loki 사용 안 함. 메트릭은 Prometheus, 로그는 CloudWatch로 완전히 분리.

### Grafana 접근

외부 포트 미노출. SSM 포트포워딩으로 접근:
```bash
aws ssm start-session \
  --target <instance-id> \
  --document-name AWS-StartPortForwardingSession \
  --parameters '{"portNumber":["3000"],"localPortNumber":["3000"]}'
# → localhost:3000 접속
```

---

## 9. 배포 파이프라인

```
[GitHub Actions]
  1. ./gradlew build
  2. docker build → ECR push (re-caring-api)
  3. aws ecs update-service --force-new-deployment

[ECS]
  4. 신규 Task 기동 (awsvpc, 새 ENI 할당)
  5. Cloud Map에 app.local 등록
  6. 헬스체크 통과
  7. Nginx가 app.local TTL 10s → 신규 Task로 트래픽 전환
  8. 구 Task Cloud Map 해제 → 구 Task 종료
```

`minimum_healthy_percent=100 / maximum_percent=200` 설정으로 무중단 보장.

---

## 10. GPS 핵심 데이터 흐름

```
[보호대상자 앱]
  → HTTPS POST /api/v1/location/gps
  → Nginx → Spring App

Spring App 4가지 동시 처리:
  ① PostgreSQL INSERT    GPS 이력 영구 저장
  ② Redis SET            gps:latest:{memberKey} TTL 5분
  ③ SSE push             접속 중인 보호자에게 실시간 전송
  ④ SQS send             gps-llm-queue → LSTM 컨테이너

[보호자 앱 — 실시간]
  → SSE 연결 /api/v1/location/stream/{wardKey}
  → Redis에서 초기값 로드 (Cache Miss 시 DB fallback)
  → GPS 수신마다 SSE push

[보호자 앱 — 경로 히스토리]
  → REST GET /api/v1/location/history/{wardKey}
  → PostgreSQL 날짜 범위 쿼리
```

### LLM 이상감지 흐름

```
SQS gps-llm-queue
  → LSTM 컨테이너 폴링 (30초 간격, Private Subnet ap-northeast-2c)
  → GPS 패턴 분석
  → 이상 감지 시: HTTP POST → Spring App /internal/anomaly
  → Spring App → FCM 푸시 알림 → 보호자
```

---

## 11. AWS 관리형 서비스 목록

| 서비스 | 용도 | 비고 |
|--------|------|------|
| SQS (gps-llm-queue) | GPS → LSTM 비동기 전달 | Standard Queue, 가시성 타임아웃 60초 |
| ECR | 컨테이너 이미지 저장 | re-caring-api, recaring-nginx, recaring-redis, recaring-certbot |
| EFS | Redis AOF + SSL 인증서 | Redis와 Certbot/Nginx 공유 |
| CloudWatch Logs | Spring 로그 (/ecs/recaring) | awslogs 드라이버, 무료 티어 5GB/월 |
| SSM Parameter Store | DB/JWT/API 시크릿 관리 | Standard (무료) |
| EventBridge | Certbot 갱신 스케줄 | 주기적 Fargate Task 기동 |

---

## 12. 비용

| 항목 | 스펙 | 월 비용 |
|------|------|--------|
| EC2 | t3.medium | ~$30 |
| RDS | db.t4g.micro + gp3 20GB | ~$15 |
| EFS | Redis AOF + 인증서 | ~$1 |
| SQS | Standard Queue | ~$0.5 |
| ECR | 이미지 저장 | ~$0.5 |
| CloudWatch Logs | 무료 티어 | ~$0 |
| **합계** | | **~$47/월** |

---

## 13. 장애 격리

| 장애 | 영향 | 복구 |
|------|------|------|
| Spring App 크래시 | API 30~60초 불가 | ECS 자동 재시작 |
| Redis 크래시 | 로그인/GPS 캐시 일시 불가 | ECS 재시작 + EFS AOF 복구 (30~60초) |
| RDS 크래시 | GPS 저장 실패 | RDS 자동 복구 (수분 소요, Multi-AZ 미적용) |
| LSTM 크래시 | 이상감지 알림만 불가 | ECS 자동 재시작, GPS 수신은 정상 |
| Monitoring 크래시 | Grafana 화면만 불가 | ECS 자동 재시작 |
| EC2 크래시 | 전체 영향 | EC2 Auto Recovery (MVP 수준 감수) |
| SQS 장애 | LLM 분석 불가 | AWS 관리형 Multi-AZ 자동 복구 |

---

## 14. EC2 접근 방법

SSH 없음. SSM Session Manager만 사용.

```bash
# 인스턴스 ID 조회 (태그 기반, 하드코딩 금지)
aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=recaring-app-server" \
            "Name=instance-state-name,Values=running" \
  --query "Reservations[0].Instances[0].InstanceId" \
  --output text

# EC2 쉘 접속
aws ssm start-session --target <instance-id>

# 컨테이너 상태 확인
docker ps --format "table {{.Names}}\t{{.Ports}}\t{{.Status}}"
```
