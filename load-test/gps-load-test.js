import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = {
  vus: 100,
  duration: '10m',
};

// 실행 전: export WARD_TOKEN="Bearer <your-jwt-token>"
const TOKEN = __ENV.WARD_TOKEN;

function randomCoord(base, range) {
  return base + (Math.random() - 0.5) * range;
}

export default function () {
  const payload = JSON.stringify({
    latitude: randomCoord(37.5665, 0.05),
    longitude: randomCoord(126.9780, 0.05),
  });

  const res = http.post('http://localhost:8080/api/v1/location/gps', payload, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': TOKEN,
    },
  });

  check(res, {
    'status 200': (r) => r.status === 200,
  });

  sleep(30);
}
