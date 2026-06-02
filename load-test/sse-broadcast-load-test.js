import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Gauge, Trend, Rate } from 'k6/metrics';

const GUARDIAN_TOKEN  = __ENV.GUARDIAN_TOKEN;
const DEVICE_TOKEN    = __ENV.DEVICE_TOKEN;
const WARD_KEY        = __ENV.WARD_KEY;
const BASE_URL        = __ENV.BASE_URL || 'http://localhost:8080';
const GPS_INTERVAL_S  = parseFloat(__ENV.GPS_INTERVAL_S || '3');

// SSE subscriber 지표
const sseErrors        = new Counter('sse_errors');
const sseDrops         = new Counter('sse_drops');
const activeConns      = new Gauge('sse_active_connections');
const connectDuration  = new Trend('sse_connect_duration_ms', true);

// GPS publisher 지표 — 핵심: 구독자 증가에 따라 p95가 선형으로 늘면 broadcast 블로킹 확증
const gpsPostDuration  = new Trend('gps_post_duration_ms', true);
const gpsSuccess       = new Counter('gps_post_success');
const gpsFailed        = new Counter('gps_post_failed');

// 총 시나리오 시간 = 30s+1m+2m+1m+2m+1m+3m+30s = 11m
export const options = {
  scenarios: {
    // 시나리오 1: SSE 구독자 — 기존 스크립트와 동일한 ramp 프로파일
    subscribers: {
      executor: 'ramping-vus',
      exec: 'subscriber',
      stages: [
        { duration: '30s', target: 100  },  // warm-up
        { duration: '1m',  target: 500  },  // 500 구독자
        { duration: '2m',  target: 500  },  // soak 500
        { duration: '1m',  target: 1000 },  // 1000 구독자
        { duration: '2m',  target: 1000 },  // soak 1000
        { duration: '1m',  target: 1500 },  // 1500 구독자
        { duration: '3m',  target: 1500 },  // soak 1500 — 한계점 탐색
        { duration: '30s', target: 0    },  // ramp-down
      ],
      gracefulStop: '30s',
    },

    // 시나리오 2: GPS 발행자 — 1 VU가 GPS_INTERVAL_S 간격으로 GPS POST
    // broadcast()가 subscriber 수에 비례해 이 요청을 블로킹하는 걸 gps_post_duration_ms로 관찰
    publisher: {
      executor: 'constant-vus',
      exec: 'gpsPublisher',
      vus: 1,
      duration: '11m',
    },
  },

  thresholds: {
    'sse_errors':          ['count < 75'],
    'sse_drops':           ['count < 75'],
    'gps_post_failed':     ['count < 5'],
    // p95 임계값: 구독자 0일 때 ~50ms 기대, 1500명이면 훨씬 초과 → 실패로 현 구조 한계 시각화
    'gps_post_duration_ms': ['p(95) < 2000'],
  },
};

// ────────────────────────────────────────────
// 시나리오 1: SSE 구독자
// ────────────────────────────────────────────
export function subscriber() {
  const before = Date.now();

  const res = http.get(
    `${BASE_URL}/api/v1/location/stream/${WARD_KEY}`,
    {
      headers: {
        'Accept': 'text/event-stream',
        'Authorization': GUARDIAN_TOKEN,
        'Cache-Control': 'no-cache',
      },
      timeout: '24m',
      responseType: 'none',
      tags: { name: 'sse_stream' },
    }
  );

  connectDuration.add(Date.now() - before);

  const ok = check(res, {
    'SSE connected (200)': (r) => r.status === 200,
    'not 401':             (r) => r.status !== 401,
    'not 403':             (r) => r.status !== 403,
    'not 500':             (r) => r.status !== 500,
  });

  if (!ok) {
    if (res.status === 0) sseDrops.add(1);
    else sseErrors.add(1);
  } else {
    activeConns.add(1);
  }
}

// ────────────────────────────────────────────
// 시나리오 2: GPS 발행자
// ────────────────────────────────────────────
export function gpsPublisher() {
  // 서울 근교 소폭 랜덤 좌표 (고정 좌표면 DB 인덱스 캐시 효과로 왜곡될 수 있음)
  const lat = 37.5665 + (Math.random() - 0.5) * 0.01;
  const lng = 126.9780 + (Math.random() - 0.5) * 0.01;

  const before = Date.now();

  const res = http.post(
    `${BASE_URL}/api/v1/location/gps`,
    JSON.stringify({ latitude: lat, longitude: lng }),
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': DEVICE_TOKEN,   // "Device <device-token>"
      },
      tags: { name: 'gps_post' },
    }
  );

  gpsPostDuration.add(Date.now() - before);

  const ok = check(res, {
    'GPS POST 200': (r) => r.status === 200,
  });

  if (ok) gpsSuccess.add(1);
  else     gpsFailed.add(1);

  sleep(GPS_INTERVAL_S);
}
