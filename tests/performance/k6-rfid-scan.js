import http from 'k6/http';
import { check, sleep } from 'k6';

// =========================================================================
// K6 LOAD BENCHMARK: 1,000 CONCURRENT RFID SCAN QUERIES
// SLA Target: Sub-10ms target, assert p99 response time < 20ms
// =========================================================================

export const options = {
  stages: [
    { duration: '10s', target: 200 },  // Ramp up to 200 users
    { duration: '30s', target: 1000 }, // Sustained load at 1,000 concurrent VUs
    { duration: '10s', target: 0 },    // Ramp down
  ],
  thresholds: {
    // Assert 99th percentile response time is under 20ms per specification
    http_req_duration: ['p(99)<20'],
    // Assert error rate is under 0.1%
    http_req_failed: ['rate<0.001'],
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080/api/v1';

// Seeded JWT Bearer Token for FARMHAND role
const AUTH_TOKEN = __ENV.AUTH_TOKEN || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...';

const sampleTags = ['COW-804', 'COW-805', 'COW-310', 'COW-311', 'PIG-102', 'HEN-221'];

export default function () {
  const randomTag = sampleTags[Math.floor(Math.random() * sampleTags.length)];
  const url = `${BASE_URL}/animals/scan/${randomTag}`;

  const params = {
    headers: {
      'Authorization': `Bearer ${AUTH_TOKEN}`,
      'Accept': 'application/json',
    },
    tags: { name: 'RFID_SubSecond_Scan' },
  };

  const res = http.get(url, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'has tagId': (r) => JSON.parse(r.body).tagId !== undefined,
    'has MRL status': (r) => JSON.parse(r.body).status !== undefined,
    'response time < 20ms': (r) => r.timings.duration < 20,
  });

  sleep(0.05); // 50ms pacing between scans
}
