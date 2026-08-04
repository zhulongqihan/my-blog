import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'https://cyruszhang.online';

export const options = {
  vus: Number(__ENV.VUS || 5),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1500'],
  },
};

export default function () {
  const responses = http.batch([
    ['GET', `${baseUrl}/api/articles?page=0&size=10`],
    ['GET', `${baseUrl}/api/articles/archive`],
    ['GET', `${baseUrl}/api/articles/54`],
  ]);

  responses.forEach(response => check(response, {
    'read path returns 200': item => item.status === 200,
  }));
  sleep(1);
}
