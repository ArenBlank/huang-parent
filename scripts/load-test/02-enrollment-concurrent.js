import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

const tokens = new SharedArray('tokens', function () {
  return JSON.parse(open('./data/test-tokens.json'));
});

const respTime = new Trend('enroll_resp_ms');
const enrollSuccess = new Counter('enroll_success');
const enrollDuplicate = new Counter('enroll_duplicate');
const enrollRateLimited = new Counter('enroll_rate_limited');
const enrollFull = new Counter('enroll_full');
const enrollFailed = new Counter('enroll_failed');

export const options = {
  stages: [
    { duration: '5s',  target: 50 },
    { duration: '10s', target: 100 },
    { duration: '10s', target: 100 },
    { duration: '5s',  target: 0 },
  ],
  thresholds: {
    'http_req_duration': ['p(95)<5000'],
    'http_req_failed': ['rate<0.30'],
  },
};

const BASE = 'http://localhost:8093';
const SCHEDULE_ID = 2;

export default function () {
  if (tokens.length === 0) return;

  const idx = Math.floor(Math.random() * tokens.length);
  const token = tokens[idx];

  const res = http.post(
    `${BASE}/app/course/enroll`,
    JSON.stringify({ scheduleId: SCHEDULE_ID }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      timeout: '10s',
    }
  );

  respTime.add(res.timings.duration);

  try {
    const body = JSON.parse(res.body);
    const msg = body.message || '';
    const code = body.code;

    check(res, { 'got response': (r) => r.status > 0 });

    if (code === 200) {
      enrollSuccess.add(1);
    } else if (msg.includes('重复提交') || msg.includes('duplicate')) {
      enrollDuplicate.add(1);
    } else if (msg.includes('频繁') || msg.includes('frequent')) {
      enrollRateLimited.add(1);
    } else if (msg.includes('满') || msg.includes('full') || msg.includes('已满')) {
      enrollFull.add(1);
    } else {
      enrollFailed.add(1);
    }
  } catch (e) {
    enrollFailed.add(1);
  }

  sleep(0.05);
}

export function handleSummary(data) {
  const metrics = data.metrics;
  return {
    'scripts/load-test/results/02-enrollment-summary.json': JSON.stringify(data, null, 2),
    stdout: `
=============================================================
  COURSE ENROLLMENT CONCURRENT TEST
  Guard: @IdempotentSubmit(5s) + @RateLimit(5/30s) + DB
=============================================================
  Total Requests:     ${metrics.http_reqs?.values?.count || 0}
  Peak VUs:           ${metrics.vus_max?.values?.value || 0}
  QPS:                ${(metrics.http_reqs?.values?.rate || 0).toFixed(1)} req/s
  P50:                ${(metrics.http_req_duration?.values?.p(50) || 0).toFixed(1)}ms
  P95:                ${(metrics.http_req_duration?.values?.p(95) || 0).toFixed(1)}ms
  P99:                ${(metrics.http_req_duration?.values?.p(99) || 0).toFixed(1)}ms
  Avg:                ${(metrics.http_req_duration?.values?.avg || 0).toFixed(1)}ms
  ---
  Enroll Success:     ${metrics.enroll_success?.values?.count || 0}
  Rate Limited:       ${metrics.enroll_rate_limited?.values?.count || 0}
  Duplicate Rejected: ${metrics.enroll_duplicate?.values?.count || 0}
  Schedule Full:      ${metrics.enroll_full?.values?.count || 0}
  Other Failed:       ${metrics.enroll_failed?.values?.count || 0}
=============================================================
`,
  };
}
