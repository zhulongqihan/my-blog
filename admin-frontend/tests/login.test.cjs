const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');
const axios = require('axios');
const vue = require('vue');
const pinia = require('pinia');

const sourceRoot = process.env.ADMIN_LOGIN_SOURCE_ROOT || path.join(__dirname, '../src');
const flush = () => new Promise(resolve => setImmediate(resolve));
const gate = () => {
  let resolve, reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
};
const admin = { id: 7, username: 'local-fixture', nickname: 'Local fixture', role: 'ADMIN', token: 'new-fixture-token' };
const response = (config, data = admin) => ({
  status: 200, statusText: 'OK', headers: {}, config,
  data: { code: 200, message: 'ok', data },
});

// Exercise the actual request interceptor, auth API, Pinia store and Login script.
// The fake adapter is the only network boundary; no request leaves this process.
function harness(adapter = config => response(config)) {
  const messages = [], redirects = [], calls = [];
  const entries = new Map([
    ['admin_token', 'expired-fixture-token'],
    ['admin_user', JSON.stringify({ ...admin, token: 'expired-fixture-token' })],
  ]);
  const localStorage = {
    getItem: key => entries.get(key) ?? null,
    setItem: (key, value) => entries.set(key, value),
    removeItem: key => entries.delete(key),
  };
  const taskPinia = pinia.createPinia();
  const router = { push: async target => { redirects.push(target); } };
  const modules = new Map();
  const context = vm.createContext({ console, localStorage });
  const execute = (source, filename) => {
    const module = { exports: {} };
    const js = ts.transpileModule(source.replaceAll('import.meta.env', '({})'), {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true },
      fileName: filename,
    }).outputText;
    const run = vm.runInContext(`(function(require,module,exports){${js}\n})`, context);
    run(dependency, module, module.exports);
    return module.exports;
  };
  const load = relative => {
    if (!modules.has(relative)) {
      modules.set(relative, execute(fs.readFileSync(path.join(sourceRoot, relative), 'utf8'), relative));
    }
    return modules.get(relative);
  };
  const dependency = name => {
    if (name === 'vue') return vue;
    if (name === 'pinia') return {
      ...pinia,
      defineStore: (...args) => {
        const useStore = pinia.defineStore(...args);
        return () => useStore(taskPinia);
      },
    };
    if (name === 'axios') return axios;
    if (name === 'vue-router') return { useRouter: () => router };
    if (name === '@/router') return router;
    if (name.includes('element-plus')) return {
      ElMessage: Object.fromEntries(['error', 'warning', 'success'].map(kind => [kind, message => messages.push({ kind, message })])),
    };
    if (name.startsWith('@/')) return load(name.slice(2) + '.ts');
    throw new Error('Unexpected dependency: ' + name);
  };
  const requestModule = load('utils/request.ts');
  requestModule.default.defaults.adapter = async config => {
    calls.push(config);
    return adapter(config, calls.length);
  };
  const script = fs.readFileSync(path.join(sourceRoot, 'views/Login.vue'), 'utf8')
    .match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1];
  const h = execute(script + '\nexport { handleLogin, formRef, loginForm, loading };', 'Login.ts');
  h.formRef.value = { validate: async () => true };
  Object.assign(h.loginForm, { username: 'local-fixture', password: 'fake-only-never-a-real-password' });
  return {
    ...h, calls, messages, redirects, entries,
    request: requestModule.default,
    isRequestErrorReported: requestModule.isRequestErrorReported,
    errors: () => messages.filter(message => message.kind === 'error').map(message => message.message),
  };
}

test('login does not attach a stored token, while /me, logout and other endpoints retain authentication', async () => {
  const f = harness();
  await f.handleLogin();
  assert.equal(f.calls[0].url, '/auth/login');
  assert.equal(f.calls[0].headers.get('Authorization'), undefined);
  assert.equal(f.entries.get('admin_token'), admin.token);
  assert.deepEqual(f.redirects, ['/dashboard']);
  for (const url of ['/auth/me', '/auth/logout', '/auth/login-extra', '/admin/articles']) {
    await f.request.get(url);
    assert.equal(f.calls.at(-1).headers.get('Authorization'), `Bearer ${admin.token}`);
  }
  // A preconfigured authorization header must not restore the stale-token path either.
  f.request.defaults.headers.common.authorization = 'Bearer stale-default-fixture';
  await f.request.post('/auth/login', { username: 'fake', password: 'fake' });
  assert.equal(f.calls.at(-1).headers.get('Authorization'), undefined);
});

for (const code of ['ECONNABORTED', 'ETIMEDOUT']) {
  test(`a ${code} login failure produces one Chinese message and no automatic retry`, async () => {
    const f = harness(config => { throw new axios.AxiosError('timeout of 15000ms exceeded', code, config); });
    await f.handleLogin();
    assert.equal(f.calls.length, 1);
    assert.equal(f.request.defaults.timeout, 15000);
    assert.equal(f.errors().length, 1);
    assert.match(f.errors()[0], /请求超时/);
    assert.doesNotMatch(f.errors()[0], /timeout of|exceeded/);
    assert.equal(f.loading.value, false);
    assert.deepEqual(f.redirects, []);
  });
}

test('reported failures preserve the original Axios error code, response and identity', async () => {
  const original = new axios.AxiosError('fake network timeout', 'ECONNABORTED');
  original.response = { status: 503, data: { message: '本地服务暂不可用' } };
  const f = harness(() => { throw original; });
  await assert.rejects(f.request.get('/admin/articles'), error => {
    assert.equal(error, original);
    assert.equal(error.code, 'ECONNABORTED');
    assert.equal(error.response.status, 503);
    assert.equal(f.isRequestErrorReported(error), true);
    return true;
  });
});

test('business login errors are reported once, while a local non-admin rejection remains visible', async () => {
  const rejected = harness(config => ({ ...response(config), data: { code: 400, message: '本地模拟登录失败' } }));
  await rejected.handleLogin();
  assert.deepEqual(rejected.errors(), ['本地模拟登录失败']);
  const noRole = harness(config => response(config, { ...admin, role: 'USER' }));
  await noRole.handleLogin();
  assert.deepEqual(noRole.errors(), ['该账号没有管理员权限']);
  assert.equal(noRole.entries.get('admin_token'), 'expired-fixture-token');
  assert.deepEqual(noRole.redirects, []);
});

test('HTTP 401 and 403 login failures produce one message and preserve existing auth semantics', async () => {
  for (const status of [401, 403]) {
    const f = harness(config => {
      throw new axios.AxiosError('Request failed with status code ' + status, 'ERR_BAD_REQUEST', config, {}, { status, data: {} });
    });
    await f.handleLogin();
    assert.equal(f.errors().length, 1);
    assert.equal(f.errors()[0], status === 401 ? '登录已过期，请重新登录' : '没有操作权限');
    assert.equal(f.entries.has('admin_token'), status !== 401);
    assert.deepEqual(f.redirects, status === 401 ? ['/login'] : []);
  }
});

test('the login lock starts before validation and covers Enter events until the request completes', async () => {
  const validation = gate(), pending = gate();
  const f = harness(() => pending.promise);
  let validations = 0;
  f.formRef.value = { validate: () => { validations++; return validation.promise; } };
  const first = f.handleLogin();
  const second = f.handleLogin();
  assert.equal(f.loading.value, true);
  assert.equal(validations, 1);
  validation.resolve(true);
  await flush();
  const third = f.handleLogin();
  await flush();
  assert.equal(f.calls.length, 1);
  pending.resolve(response(f.calls[0]));
  await Promise.all([first, second, third]);
  assert.equal(f.loading.value, false);
  assert.deepEqual(f.redirects, ['/dashboard']);
});

test('invalid form input sends no login request, releases the lock and allows correction', async () => {
  const f = harness();
  f.formRef.value = { validate: async () => { throw { username: [{ message: '请输入用户名' }] }; } };
  await f.handleLogin();
  assert.equal(f.calls.length, 0);
  assert.equal(f.loading.value, false);
  assert.deepEqual(f.errors(), []);
  f.formRef.value = { validate: async () => true };
  await f.handleLogin();
  assert.equal(f.calls.length, 1);
  assert.equal(f.loading.value, false);
});

test('after a timed-out attempt an explicit second click can succeed without retrying the first', async () => {
  const f = harness((config, count) => {
    if (count === 1) throw new axios.AxiosError('timeout of 15000ms exceeded', 'ECONNABORTED', config);
    return response(config);
  });
  await f.handleLogin();
  assert.equal(f.calls.length, 1);
  await f.handleLogin();
  assert.equal(f.calls.length, 2);
  assert.equal(f.errors().length, 1);
  assert.equal(f.messages.filter(message => message.kind === 'success').length, 1);
  assert.deepEqual(f.redirects, ['/dashboard']);
});
