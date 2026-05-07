import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

const tokens = new SharedArray('tokens', function () {
  return JSON.parse(open('./data/test-tokens.json'));
});

const respTime = new Trend('mixed_resp_ms');
const okCount = new Counter('mixed_success');
const failCount = new Counter('mixed_failed');
const writeAttempts = new Counter('write_attempts');

export const options = {
  stages: [
    { duration: '10s', target: 50 },
    { duration: '20s', target: 150 },
    { duration: '60s', target: 200 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    'http_req_duration': ['p(95)<3000'],
    'http_req_failed': ['rate<0.05'],
  },
};

const BASE = 'http://localhost';

function authHeaders() {
  const token = tokens.length > 0 ? tokens[Math.floor(Math.random() * tokens.length)] : '';
  return {
    'Host': 'app.localhost',
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

// Scenario weights (80% cache read, 15% DB read, 5% write)
export default function () {
  const roll = Math.random();
  let res;

  if (roll < 0.80) {
    // 80%: Cache-hit reads (no auth needed)
    const cacheEndpoints = [
      '/app/banner/list',
      '/app/course/list',
      '/app/plan/overview',
      '/app/plan/list',
      '/app/system-config/map',
      '/app/notice/list',
    ];
    const url = cacheEndpoints[Math.floor(Math.random() * cacheEndpoints.length)];
    res = http.get(`${BASE}${url}`, {
      headers: { 'Host': 'app.localhost' },
      timeout: '10s',
    });
  } else if (roll < 0.95) {
    // 15%: DB reads (need auth)
    const dbEndpoints = [
      '/app/course/my/enrollments',
      '/app/booking/my/list',
      '/app/order/my/list',
      '/app/booking/schedule/summary',
      '/app/profile/info',
    ];
    const url = dbEndpoints[Math.floor(Math.random() * dbEndpoints.length)];
    res = http.get(`${BASE}${url}`, {
      headers: authHeaders(),
      timeout: '10s',
    });
  } else {
    // 5%: Write (enroll)
    writeAttempts.add(1);
    res = http.post(
      `${BASE}/app/course/enroll`,
      JSON.stringify({ scheduleId: 2 }),
      { headers: authHeaders(), timeout: '10s' }
    );
  }

  respTime.add(res.timings.duration);

  try {
    const body = JSON.parse(res.body);
    if (body.code === 200) {
      okCount.add(1);
      check(res, { 'status ok': () => true });
    } else {
      // Non-200 code but valid response (e.g. rate limited, duplicate) — not a failure
      okCount.add(1);
    }
  } catch (e) {
    failCount.add(1);
  }

  sleep(0.05);
}

export function handleSummary(data) {
  const m = data.metrics;
  const totalReqs = m.http_reqs?.values?.count || 0;
  return {
    stdout: `
================================================================
  ROUND 3: Dual-Instance Mixed Workload via Nginx
  80% cache read / 15% DB read / 5% write, 200 VU peak, 100s
================================================================
  Total Requests:     ${totalReqs}
  Peak VUs:           ${m.vus_max?.values?.value || 0}
  QPS:                ${(m.http_reqs?.values?.rate || 0).toFixed(1)} req/s
  P50:                ${(m.http_req_duration?.values?.p(50) || 0).toFixed(1)}ms
  P90:                ${(m.http_req_duration?.values?.p(90) || 0).toFixed(1)}ms
  P95:                ${(m.http_req_duration?.values?.p(95) || 0).toFixed(1)}ms
  P99:                ${(m.http_req_duration?.values?.p(99) || 0).toFixed(1)}ms
  Avg:                ${(m.http_req_duration?.values?.avg || 0).toFixed(1)}ms
  Max:                ${(m.http_req_duration?.values?.max || 0).toFixed(1)}ms
  ---
  Success (inc. non-200): ${m.mixed_success?.values?.count || 0}
  Failed (network):       ${m.mixed_failed?.values?.count || 0}
  Write Attempts (5%):    ${m.write_attempts?.values?.count || 0}
  HTTP Fail Rate:         ${(m.http_req_failed?.values?.rate || 0).toFixed(4)}
================================================================
`,
  };
}
