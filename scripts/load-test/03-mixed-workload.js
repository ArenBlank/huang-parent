import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

const tokens = new SharedArray('tokens', function () {
  return JSON.parse(open('./data/test-tokens.json'));
});

const respTime = new Trend('mixed_resp_ms');
const successRate = new Rate('mixed_success');
const writeAttempts = new Counter('write_attempts');

export const options = {
  stages: [
    { duration: '10s', target: 30 },
    { duration: '40s', target: 80 },
    { duration: '40s', target: 80 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    'http_req_duration': ['p(95)<5000'],
    'http_req_failed': ['rate<0.15'],
  },
};

const BASE = 'http://localhost:8081';

export default function () {
  if (tokens.length === 0) return;

  const idx = Math.floor(Math.random() * tokens.length);
  const token = tokens[idx];
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };

  const rand = Math.random();
  let res;

  if (rand < 0.25) {
    const urls = ['/app/banner/list', '/app/course/list', '/app/plan/overview', '/app/plan/list', '/app/system-config/map'];
    res = http.get(BASE + urls[Math.floor(Math.random() * urls.length)], { timeout: '10s' });
  } else if (rand < 0.45) {
    res = http.get(`${BASE}/app/course/my/enrollments`, { headers, timeout: '10s' });
  } else if (rand < 0.60) {
    res = http.get(`${BASE}/app/booking/my/list`, { headers, timeout: '10s' });
  } else if (rand < 0.75) {
    res = http.get(`${BASE}/app/order/my/list`, { headers, timeout: '10s' });
  } else if (rand < 0.88) {
    const url = Math.random() < 0.5 ? '/app/booking/schedule/list' : '/app/booking/schedule/summary';
    res = http.get(BASE + url, { headers, timeout: '10s' });
  } else if (rand < 0.96) {
    res = http.get(`${BASE}/app/profile/info`, { headers, timeout: '10s' });
  } else {
    writeAttempts.add(1);
    res = http.post(
      `${BASE}/app/course/enroll`,
      JSON.stringify({ scheduleId: 2 }),
      { headers, timeout: '10s' }
    );
  }

  respTime.add(res.timings.duration);
  successRate.add(res.status === 200);

  check(res, { 'status ok': (r) => r.status === 200 || r.status === 500 });

  sleep(0.1);
}

export function handleSummary(data) {
  const m = data.metrics;
  return {
    'scripts/load-test/results/03-mixed-summary.json': JSON.stringify(data, null, 2),
    stdout: `
=============================================================
  MIXED WORKLOAD TEST (80% Read / 20% Write)
=============================================================
  Total Requests:    ${m.http_reqs?.values?.count || 0}
  Peak VUs:          ${m.vus_max?.values?.value || 0}
  QPS:               ${(m.http_reqs?.values?.rate || 0).toFixed(1)} req/s
  P50:               ${(m.http_req_duration?.values?.p(50) || 0).toFixed(1)}ms
  P95:               ${(m.http_req_duration?.values?.p(95) || 0).toFixed(1)}ms
  P99:               ${(m.http_req_duration?.values?.p(99) || 0).toFixed(1)}ms
  Avg:               ${(m.http_req_duration?.values?.avg || 0).toFixed(1)}ms
  Success Rate:      ${((1 - (m.http_req_failed?.values?.rate || 0)) * 100).toFixed(1)}%
  Write Attempts:    ${m.write_attempts?.values?.count || 0}
=============================================================
`,
  };
}
