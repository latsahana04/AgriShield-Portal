import http from 'k6/http';
import { check, sleep } from 'k6';

// =========================================================================
// K6 LOAD BENCHMARK: HYPERLEDGER FABRIC COMMIT THROUGHPUT & LATENCY
// SLA Target: Assert transaction execution latency < 2000ms (2s)
// =========================================================================

export const options = {
  stages: [
    { duration: '10s', target: 50 },   // Warm up to 50 concurrent transactions
    { duration: '30s', target: 200 },  // Sustained throughput test
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    // Assert 95th percentile execution latency is under 2000ms (2.0s)
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080/api/v1';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...';

export default function () {
  const url = `${BASE_URL}/data/clean`;

  const payload = JSON.stringify({
    rows: [
      {
        animalTagId: `COW-${Math.floor(Math.random() * 800) + 100}`,
        medicationName: 'Penicillin G Procaine',
        dosage: '10 ml',
        dosageMg: 1000.0,
        route: 'Intramuscular',
        vetLicenseId: 'VET-USA-4402',
        timestamp: new Date().toISOString(),
      },
    ],
    commitCleanToLedger: true,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${AUTH_TOKEN}`,
    },
    tags: { name: 'Fabric_Chaincode_Commit' },
  };

  const res = http.post(url, payload, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'committed to ledger': (r) => JSON.parse(r.body).committedToLedger === true,
    'execution latency < 2s': (r) => r.timings.duration < 2000,
  });

  sleep(0.5);
}
