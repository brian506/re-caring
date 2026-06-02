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

export const options = {
  scenarios: {
    sse_ramp: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 100  },  // warm-up
        { duration: '1m',  target: 500  },  // 500 연결
        { duration: '2m',  target: 500  },  // soak 500
        { duration: '1m',  target: 1000 },  // 1000 연결
        { duration: '2m',  target: 1000 },  // soak 1000
        { duration: '1m',  target: 1500 },  // 1500 연결
        { duration: '3m',  target: 1500 },  // soak 1500 — 한계점 탐색
        { duration: '30s', target: 0    },  // ramp-down
      ],
      gracefulStop: '30s',
    },
  },
  thresholds: {
    'sse_errors': ['count < 75'],   // 전체 연결 시도의 ~1% 미만
    'sse_drops':  ['count < 75'],
  },
};

export default function () {
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
    'not 401': (r) => r.status !== 401,
    'not 403': (r) => r.status !== 403,
    'not 500': (r) => r.status !== 500,
  });

  if (!ok) {
    if (res.status === 0) {
      sseDrops.add(1);
    } else {
      sseErrors.add(1);
    }
  } else {
    activeConns.add(1);
  }
}
