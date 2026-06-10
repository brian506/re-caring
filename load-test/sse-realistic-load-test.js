import http from 'k6/http';
import { check } from 'k6';
import { Counter, Gauge, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import exec from 'k6/execution';

// ─────────────────────────────────────────────────────────────
// 운영 분포 부하 테스트: N ward × G guardian
//
// 두 시나리오를 동시에 돌려 운영 부하를 재현한다.
//   sse_subscribers : 보호자가 SSE 연결을 "유지"  → Old Gen / 메모리 천장 축
//   gps_publishers  : 보호대상자가 일정 RPS로 GPS POST → Young GC / CPU / broadcast 축
//
// 실행 전:
//   JWT_KEY=<서버값> node realistic-data-gen.js   # dataset.json + seed.sql 생성
//   psql ... -f seed.sql                          # DB에 시드 적용
//   export BASE_URL="http://<dev-host>:8080"
//   k6 run sse-realistic-load-test.js
// ─────────────────────────────────────────────────────────────

const BASE_URL       = __ENV.BASE_URL || 'http://localhost:8080';
const GPS_INTERVAL_S = parseFloat(__ENV.GPS_INTERVAL_S || '5');
const DATASET        = __ENV.DATASET || './dataset.json';

// 데이터셋 (VU 간 메모리 공유) — subscribers[].token 은 generator가 미리 서명
const SUBS  = new SharedArray('subs',  () => JSON.parse(open(DATASET)).subscribers); // {wardKey, guardianKey, token}
const WARDS = new SharedArray('wards', () => JSON.parse(open(DATASET)).wards);       // {wardKey, deviceToken}

if (!SUBS[0] || !SUBS[0].token) throw new Error('dataset.json에 token이 없습니다. JWT_KEY를 지정하고 realistic-data-gen.js를 다시 실행하세요.');

// ── 지표 ──────────────────────────────────────────────────────
const sseErrors       = new Counter('sse_errors');        // 4xx/5xx 등 응답 에러
const sseDrops        = new Counter('sse_drops');         // status 0 (연결 실패/리셋)
const activeConns     = new Gauge('sse_active_connections');
const connectDuration = new Trend('sse_connect_duration_ms', true);

const gpsPostDuration = new Trend('gps_post_duration_ms', true); // ★ broadcast 영향 핵심 지표
const gpsSuccess      = new Counter('gps_post_success');
const gpsFailed       = new Counter('gps_post_failed');

// ── SSE ramp 프로파일 (breakpoint 탐색) ───────────────────────
// 각 단계 5분 soak → GC baseline 우상향 / 누수 발현 관측.
// 주의: 최대 target은 dataset의 subscribers 수(N*G) 이하여야 한다.
const SSE_STAGES = [
  { duration: '1m', target: 500  },  // 100 ward
  { duration: '5m', target: 500  },
  { duration: '1m', target: 1000 },  // 200 ward
  { duration: '5m', target: 1000 },
  { duration: '1m', target: 2000 },  // 400 ward — 최대 목표
  { duration: '5m', target: 2000 },
  { duration: '1m', target: 0    },  // ramp-down
];

// publisher 전체 실행 시간 = SSE 단계 합 (자동 계산)
const toSec = (d) => (d.endsWith('m') ? parseInt(d) * 60 : parseInt(d));
const TOTAL_SEC = SSE_STAGES.reduce((a, s) => a + toSec(s.duration), 0);

// GPS 발행률: ward 전체가 GPS_INTERVAL_S마다 1회 → ward수 / interval (RPS)
const GPS_RATE = parseInt(__ENV.GPS_RATE) || Math.max(1, Math.ceil(WARDS.length / GPS_INTERVAL_S));

export const options = {
  scenarios: {
    sse_subscribers: {
      executor: 'ramping-vus',
      exec: 'subscribe',
      startVUs: 0,
      stages: SSE_STAGES,
      gracefulRampDown: '10s',
      gracefulStop: '30s',
    },
    gps_publishers: {
      executor: 'constant-arrival-rate',  // ★ VU가 아닌 "초당 건수" 고정 → 서버 지연이 큐 적체로 정직하게 드러남
      exec: 'publish',
      rate: GPS_RATE,
      timeUnit: '1s',
      duration: `${TOTAL_SEC}s`,
      preAllocatedVUs: GPS_RATE,          // 평상시 충분
      maxVUs: GPS_RATE * 5,               // STW 스파이크 시 지연 흡수 (dropped_iterations 뜨면 상향)
    },
  },
  // SLO — 하나라도 깨지는 연결 수가 capacity 상한
  thresholds: {
    'gps_post_duration_ms': ['p(95)<500', 'p(99)<1000'],
    'sse_drops':            ['count<150'],   // 전체 시도의 ~1%
    'gps_post_failed':      ['rate<0.01'],
    'http_req_failed':      ['rate<0.01'],
  },
};

// ── 시나리오 1: 보호자 SSE 구독 (연결 유지) ──────────────────
export function subscribe() {
  // iterationInTest는 시나리오 내 연결마다 고유 → 동시 연결 슬롯 분산
  const s = SUBS[exec.scenario.iterationInTest % SUBS.length];
  const before = Date.now();

  const res = http.get(`${BASE_URL}/api/v1/location/stream/${s.wardKey}`, {
    headers: {
      'Accept': 'text/event-stream',
      'Authorization': s.token,
      'Cache-Control': 'no-cache',
    },
    timeout: '60m',          // 서버 SSE 타임아웃(5분)에 의해 끊기면 재연결 → 자연 churn 발생
    responseType: 'none',
    tags: { name: 'sse_stream' },
  });

  connectDuration.add(Date.now() - before);

  const ok = check(res, {
    'SSE connected (200)': (r) => r.status === 200,
    'not 401': (r) => r.status !== 401,
    'not 403': (r) => r.status !== 403,
    'not 500': (r) => r.status !== 500,
  });

  if (!ok) {
    if (res.status === 0) sseDrops.add(1);
    else sseErrors.add(1);
  } else {
    activeConns.add(1);
  }
}

// ── 시나리오 2: 보호대상자 GPS POST (broadcast 트리거) ────────
export function publish() {
  const w = WARDS[exec.scenario.iterationInTest % WARDS.length]; // 전체 ward 라운드로빈
  const lat = 37.5665 + (Math.random() - 0.5) * 0.01;
  const lng = 126.9780 + (Math.random() - 0.5) * 0.01;
  const before = Date.now();

  const res = http.post(
    `${BASE_URL}/api/v1/location/gps`,
    JSON.stringify({ latitude: lat, longitude: lng }),
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Device ${w.deviceToken}`,
      },
      tags: { name: 'gps_post' },
    }
  );

  gpsPostDuration.add(Date.now() - before);

  if (check(res, { 'GPS POST 200': (r) => r.status === 200 })) gpsSuccess.add(1);
  else gpsFailed.add(1);
}

export function setup() {
  console.log(`[부하 구성] subscribers=${SUBS.length} | wards=${WARDS.length} | GPS_RATE=${GPS_RATE}/s | 총 ${TOTAL_SEC}s`);
  const maxTarget = Math.max(...SSE_STAGES.map((s) => s.target));
  if (maxTarget > SUBS.length) {
    console.warn(`[경고] 최대 SSE target(${maxTarget}) > seeded subscribers(${SUBS.length}). dataset을 더 크게 생성하세요.`);
  }
}
