import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const respTime = new Trend('cache_read_ms');
const success = new Rate('cache_read_success');

export const options = {
  stages: [
    { duration: '10s', target: 100 },
    { duration: '20s', target: 300 },
    { duration: '30s', target: 500 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    'http_req_duration': ['p(95)<2000'],
    'http_req_failed': ['rate<0.10'],
  },
};

const BASE = 'http://localhost:8093';

export default function () {
  const endpoints = [
    '/app/banner/list',
    '/app/course/list',
    '/app/plan/overview',
    '/app/plan/list',
    '/app/system-config/map',
    '/app/notice/list',
  ];
  const url = BASE + endpoints[Math.floor(Math.random() * endpoints.length)];

  const res = http.get(url, { timeout: '10s' });

  check(res, {
    'status 200': (r) => r.status === 200,
  });

  respTime.add(res.timings.duration);
  success.add(res.status === 200);

  sleep(0.05);
}

export function handleSummary(data) {
  return {
    'scripts/load-test/results/01-cache-read-summary.json': JSON.stringify(data, null, 2),
    stdout: `
=== Cache Read Test Summary ===
Total Requests:  ${data.metrics.http_reqs?.values?.count || 0}
Peak VUs:        ${data.metrics.vus_max?.values?.value || 0}
QPS:             ${(data.metrics.http_reqs?.values?.rate || 0).toFixed(1)} req/s
P50:             ${(data.metrics.http_req_duration?.values?.p(50) || 0).toFixed(1)}ms
P95:             ${(data.metrics.http_req_duration?.values?.p(95) || 0).toFixed(1)}ms
P99:             ${(data.metrics.http_req_duration?.values?.p(99) || 0).toFixed(1)}ms
Avg:             ${(data.metrics.http_req_duration?.values?.avg || 0).toFixed(1)}ms
Failed Rate:     ${(data.metrics.http_req_failed?.values?.rate || 0).toFixed(4)}
===============================
`,
  };
}
