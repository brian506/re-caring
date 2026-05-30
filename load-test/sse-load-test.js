import http from 'k6/http';
import { check } from 'k6';
import { Counter, Gauge, Trend } from 'k6/metrics';

// 실행 전:
//   export GUARDIAN_TOKEN="Bearer <guardian-jwt>"
//   export WARD_KEY="<ward-member-key>"
//   export BASE_URL="http://localhost:8080"  (기본값)

const GUARDIAN_TOKEN = __ENV.GUARDIAN_TOKEN;
const WARD_KEY = __ENV.WARD_KEY;
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const sseErrors = new Counter('sse_errors');
const sseDrops = new Counter('sse_drops');
const activeConns = new Gauge('sse_active_connections');
const connectDuration = new Trend('sse_connect_duration_ms', true);

// 단계적으로 동시 연결 수 증가 → 서버 한계점 탐색
export const options = {
  scenarios: {
    sse_ramp: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 100 },   // warm-up
        { duration: '1m',  target: 200 },   // 200 연결
        { duration: '1m',  target: 300 },   // 300 연결
        { duration: '3m',  target: 300 },   // soak — 안정 구간 유지
        { duration: '30s', target: 0 },     // ramp-down
      ],
      gracefulStop: '30s',
    },
  },
  thresholds: {
    // 에러가 전체 연결 시도의 1% 미만이어야 함
    'sse_errors': ['count < 15'],
    // 연결 성공 여부 (check 기반)
    'checks': ['rate > 0.99'],
  },
};

export default function () {
  const before = Date.now();

  // SSE 연결 유지: timeout까지 VU가 blocking → 동시 연결 수 = VU 수
  // responseType: 'none' 으로 body 버퍼링 없이 연결만 유지
  const res = http.get(
    `${BASE_URL}/api/v1/location/stream/${WARD_KEY}`,
    {
      headers: {
        'Accept': 'text/event-stream',
        'Authorization': GUARDIAN_TOKEN,
        'Cache-Control': 'no-cache',
      },
      timeout: '24m',         // SSE timeout(30분)보다 짧게
      responseType: 'none',   // body 버퍼링 안 함 — 메모리 폭발 방지
      tags: { name: 'sse_stream' },
    }
  );

  connectDuration.add(Date.now() - before);

  const ok = check(res, {
    'SSE connected (200)': (r) => r.status === 200,
    'not 401': (r) => r.status !== 401,
    'not 403': (r) => r.status !== 403,
    'not 500': (r) => r.status !== 500,
  });

  if (!ok) {
    if (res.status === 0) {
      // 연결 자체 실패 (connection refused, timeout)
      sseDrops.add(1);
    } else {
      sseErrors.add(1);
    }
  } else {
    activeConns.add(1);
  }

  // 연결 종료 후 짧게 쉬고 재연결 (실제 클라이언트 reconnect 패턴)
  // soak 단계에서 연결이 끊기면 자동 재연결
}
