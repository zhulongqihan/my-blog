const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');
const axios = require('axios');

function client() {
  const messages = [];
  const redirects = [];
  const stored = new Map([['admin_token', 'test-only'], ['admin_user', '{}']]);
  const module = { exports: {} };
  const source = ts.transpileModule(fs.readFileSync(path.join(__dirname, '../src/utils/request.ts'), 'utf8')
    .replaceAll('import.meta.env', '({})'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true },
  }).outputText;
  vm.runInNewContext(source, {
    module, exports: module.exports,
    localStorage: { getItem: key => stored.get(key), removeItem: key => stored.delete(key) },
    require: name => {
      if (name === 'axios') return axios;
      if (name.includes('element-plus')) return { ElMessage: { error: value => messages.push(value), warning: value => messages.push(value) } };
      if (name === '@/router') return { push: value => redirects.push(value) };
      throw new Error('Unexpected dependency: ' + name);
    },
  });
  return { request: module.exports.default, messages, redirects, stored };
}

for (const code of ['ECONNABORTED', 'ETIMEDOUT']) test(`a ${code} timeout is identified without claiming the user is offline`, async () => {
  const c = client();
  c.request.defaults.adapter = async config => { throw new axios.AxiosError('timeout', code, config); };
  await assert.rejects(c.request.get('/admin/articles/82'));
  assert.match(c.messages[0], /请求超时/);
  assert.doesNotMatch(c.messages[0], /网络连接失败/);
  assert.equal(c.stored.has('admin_token'), true);
});

test('connection failures retain a distinct network message', async () => {
  const c = client();
  c.request.defaults.adapter = async config => { throw new axios.AxiosError('Network Error', 'ERR_NETWORK', config); };
  await assert.rejects(c.request.get('/admin/articles/82'));
  assert.match(c.messages[0], /网络连接失败/);
  assert.equal(c.redirects.length, 0);
});

test('an expired login still clears authentication and redirects', async () => {
  const c = client();
  c.request.defaults.adapter = async config => {
    throw new axios.AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, {}, { status: 401, data: {} });
  };
  await assert.rejects(c.request.get('/admin/articles/82'));
  assert.equal(c.stored.has('admin_token'), false);
  assert.equal(c.stored.has('admin_user'), false);
  assert.deepEqual(c.redirects, ['/login']);
});

test('a forbidden response preserves the login and reports the existing permission boundary', async () => {
  const c = client();
  c.request.defaults.adapter = async config => {
    throw new axios.AxiosError('Forbidden', 'ERR_BAD_REQUEST', config, {}, { status: 403, data: {} });
  };
  await assert.rejects(c.request.get('/admin/articles/82'));
  assert.equal(c.messages[0], '没有操作权限');
  assert.equal(c.stored.has('admin_token'), true);
});
