const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { webcrypto } = require('node:crypto');
const ts = require('typescript');
const vue = require('vue');
const sourceRoot = process.env.ARTICLE_SAVE_SOURCE_ROOT || path.join(__dirname, '../src');

const timeout = () => Object.assign(new Error('Controlled lost response'), { code: 'ECONNABORTED' });
const flush = () => new Promise(resolve => setImmediate(resolve));
const clone = value => JSON.parse(JSON.stringify(value));
const storage = () => {
  const entries = new Map();
  return { getItem: key => entries.get(key) ?? null, setItem: (key, value) => entries.set(key, value), removeItem: key => entries.delete(key) };
};

function harness({ session = storage(), owner = 7, create, update, navigate, list, browserCrypto = webcrypto } = {}) {
  const messages = [], posts = [], puts = [], redirects = [], unmountCallbacks = [];
  const local = storage();
  local.setItem('admin_user', JSON.stringify({ id: owner, username: `fixture-${owner}`, role: 'ADMIN' }));
  const route = vue.reactive({ params: {}, fullPath: '/articles/create' });
  const api = {
    createArticle: async (payload, key) => { posts.push({ payload: clone(payload), key }); return create ? create(payload, key) : { data: { id: 83 } }; },
    updateArticle: async (id, payload) => { puts.push({ id, payload: clone(payload) }); return update ? update(id, payload) : { data: { id } }; },
    getArticles: list || (async () => ({ data: { records: [], total: 0 } })),
    getArticleDetail: async id => ({ data: { id, title: 'existing', content: 'existing content', summary: '', tags: [], category: null, published: false, featured: false } }),
  };
  const context = vm.createContext({ console, crypto: browserCrypto, localStorage: local, sessionStorage: session });
  const modules = new Map();
  const load = filename => {
    if (modules.has(filename)) return modules.get(filename);
    const module = { exports: {} };
    modules.set(filename, module.exports);
    const source = fs.readFileSync(filename, 'utf8').replaceAll('import.meta.env', '({})');
    const output = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true } }).outputText;
    const run = vm.runInContext(`(function(require,module,exports){${output}\n})`, context);
    run(dependency, module, module.exports);
    return module.exports;
  };
  const dependency = name => {
    if (name === 'vue') return { ...vue, onMounted: () => {}, onBeforeUnmount: callback => unmountCallbacks.push(callback) };
    if (name === 'vue-router') return { useRoute: () => route, useRouter: () => ({ push: async target => { redirects.push(target); return navigate?.(target); } }) };
    if (name.includes('element-plus')) return { ElMessage: Object.fromEntries(['error', 'warning', 'success', 'info'].map(kind => [kind, message => messages.push({ kind, message })])), ElMessageBox: {} };
    if (name === '@/api/article') return api;
    if (name === '@/api/category') return { getCategories: async () => ({ data: [] }) };
    if (name === '@/api/tag') return { getTags: async () => ({ data: [] }) };
    if (name.startsWith('@/utils/')) return load(path.join(sourceRoot, name.slice(2) + '.ts'));
    if (name === 'md-editor-v3' || name.endsWith('.css')) return {};
    throw new Error('Unexpected dependency: ' + name);
  };
  function component(name, exposed) {
    const source = fs.readFileSync(path.join(sourceRoot, `views/${name}.vue`), 'utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1].replaceAll('import.meta.env', '({})');
    const output = ts.transpileModule(source + `\nglobalThis.h = { ${exposed} };`, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText;
    const run = vm.runInContext(`(function(require,exports){${output}\n})`, context);
    run(dependency, {});
    return context.h;
  }
  return {
    edit: async () => { const h = component('ArticleEdit', 'form, formRef, saving, handleSaveDraft, handlePublish, canSave'); h.formRef.value = { validate: async () => true }; await flush(); return h; },
    list: () => component('ArticleList', 'fetchArticles, queryParams, articles, loading, total'),
    session, local, route, messages, posts, puts, redirects,
    unmount: () => unmountCallbacks.forEach(callback => callback()),
  };
}

async function filled(options) {
  const fixture = harness(options), h = await fixture.edit();
  Object.assign(h.form, { title: 'Local draft', content: 'Original content', tagIds: [1] });
  return { ...fixture, h };
}

test('concurrent validation and save clicks send only one create request', async () => {
  let release;
  const f = await filled({ create: () => new Promise(resolve => { release = resolve; }) });
  const validators = [];
  f.h.formRef.value = { validate: () => new Promise(resolve => validators.push(resolve)) };
  const a = f.h.handleSaveDraft(), b = f.h.handlePublish();
  validators.forEach(resolve => resolve(true)); await flush();
  assert.equal(f.posts.length, 1);
  assert.equal(f.h.saving.value, true);
  release({ data: { id: 83 } }); await Promise.all([a, b]);
});

test('a lost create response replays the original payload and key before updating current edits', async () => {
  let attempts = 0;
  const f = await filled({ create: async () => { if (++attempts === 1) throw timeout(); return { data: { id: 83 } }; } });
  await f.h.handleSaveDraft();
  assert.match(f.messages.map(item => item.message).join(' '), /待确认/);
  f.h.form.title = 'Edited after timeout'; f.h.form.tagIds.push(2);
  await f.h.handlePublish();
  assert.match(f.posts[0].key, /^[0-9a-f-]{36}$/i);
  assert.equal(f.posts[1].key, f.posts[0].key);
  assert.deepEqual(f.posts[1].payload, f.posts[0].payload);
  assert.equal(f.puts.length, 1);
  assert.equal(f.puts[0].id, 83);
  assert.equal(f.puts[0].payload.title, 'Edited after timeout');
  assert.deepEqual(f.puts[0].payload.tagIds, [1, 2]);
  assert.equal(f.puts[0].payload.published, true);
});

test('refresh restores the latest input but recovers creation with its immutable original payload', async () => {
  const f = await filled({ create: async () => { throw timeout(); } });
  await f.h.handleSaveDraft();
  f.h.form.title = 'Unsaved edited title'; f.h.form.content = 'Unsaved edited content';
  await flush();
  const reload = harness({ session: f.session }), h = await reload.edit();
  assert.equal(h.form.title, 'Unsaved edited title');
  assert.equal(h.form.content, 'Unsaved edited content');
  await h.handleSaveDraft();
  assert.equal(reload.posts[0].key, f.posts[0].key);
  assert.deepEqual(reload.posts[0].payload, f.posts[0].payload);
  assert.equal(reload.puts[0].payload.title, 'Unsaved edited title');
});

test('a failed PUT after recovering the ID stays a PUT after refresh', async () => {
  let attempts = 0;
  const f = await filled({ create: async () => { if (++attempts === 1) throw timeout(); return { data: { id: 83 } }; }, update: async () => { throw timeout(); } });
  await f.h.handleSaveDraft(); f.h.form.title = 'New changes'; await f.h.handleSaveDraft();
  assert.equal(f.puts.length, 1);
  const reload = harness({ session: f.session }), h = await reload.edit();
  assert.equal(h.form.title, 'New changes');
  await h.handleSaveDraft();
  assert.equal(reload.posts.length, 0);
  assert.equal(reload.puts[0].id, 83);
});

test('navigation failure preserves the known article ID for retries and refresh', async () => {
  const f = await filled({ navigate: async () => ({ type: 4 }) });
  await f.h.handleSaveDraft(); f.h.form.title = 'After failed navigation'; await f.h.handleSaveDraft();
  assert.equal(f.posts.length, 1);
  assert.equal(f.puts[0].id, 83);
  const reload = harness({ session: f.session }), h = await reload.edit();
  assert.equal(h.form.title, 'After failed navigation');
  await h.handleSaveDraft();
  assert.equal(reload.posts.length, 0);
  assert.equal(reload.puts[0].id, 83);
});

test('successful navigation clears the recovery record so a later new article is independent', async () => {
  const f = await filled(); await f.h.handleSaveDraft();
  const next = harness({ session: f.session }), h = await next.edit();
  assert.equal(h.form.title, '');
  Object.assign(h.form, { title: 'Local draft', content: 'Same titles are allowed' });
  await h.handleSaveDraft();
  assert.notEqual(next.posts[0].key, f.posts[0].key);
});

test('one account never restores another account draft', async () => {
  const f = await filled({ create: async () => { throw timeout(); } }); await f.h.handleSaveDraft();
  const other = harness({ session: f.session, owner: 8 }), h = await other.edit();
  assert.equal(h.form.title, '');
  Object.assign(h.form, { title: 'Account 8 draft', content: 'Account 8 content' }); await h.handleSaveDraft();
  const original = harness({ session: f.session }), restored = await original.edit();
  assert.equal(restored.form.title, 'Local draft');
  assert.notEqual(other.posts[0].key, f.posts[0].key);
});

test('a recovery storage failure blocks POST instead of starting an unrecoverable write', async () => {
  const session = { getItem: () => null, setItem: () => { throw new Error('Storage quota'); }, removeItem: () => {} };
  const f = await filled({ session }); await f.h.handleSaveDraft();
  assert.equal(f.posts.length, 0);
  assert.match(f.messages.map(item => item.message).join(' '), /恢复记录/);
});

test('UUIDv4 creation works when an HTTP browser provides getRandomValues without randomUUID', async () => {
  const f = await filled({ browserCrypto: { getRandomValues: value => webcrypto.getRandomValues(value) } });
  await f.h.handleSaveDraft();
  assert.equal(f.posts.length, 1);
  assert.match(f.posts[0].key, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i);
});

test('an account change during recovery never applies the old draft with the new account credentials', async () => {
  let attempts = 0, release;
  const f = await filled({ create: async () => { if (++attempts === 1) throw timeout(); return new Promise(resolve => { release = resolve; }); } });
  await f.h.handleSaveDraft(); f.h.form.title = 'Changed before recovery';
  const saving = f.h.handleSaveDraft(); await flush();
  f.local.setItem('admin_user', JSON.stringify({ id: 8, username: 'fixture-8' }));
  release({ data: { id: 83 } }); await saving;
  assert.equal(f.puts.length, 0);
  assert.equal(f.redirects.length, 0);
});

test('an initial validation rejection permits correcting the payload without losing input', async () => {
  let attempts = 0;
  const f = await filled({ create: async () => { if (++attempts === 1) throw { response: { status: 400 } }; return { data: { id: 83 } }; } });
  await f.h.handleSaveDraft(); f.h.form.title = 'Corrected title'; await f.h.handleSaveDraft();
  assert.equal(f.posts[1].payload.title, 'Corrected title');
  assert.notEqual(f.posts[1].key, f.posts[0].key);
});

test('a validation rejection during an uncertain retry never rotates the original operation key', async () => {
  let attempts = 0;
  const f = await filled({ create: async () => { if (++attempts === 1) throw timeout(); if (attempts === 2) throw { response: { status: 400 } }; return { data: { id: 83 } }; } });
  await f.h.handleSaveDraft(); f.h.form.title = 'Current edits'; await f.h.handleSaveDraft(); await f.h.handleSaveDraft();
  assert.equal(f.posts.length, 3);
  assert.equal(f.posts[2].key, f.posts[0].key);
  assert.deepEqual(f.posts[2].payload, f.posts[0].payload);
  assert.equal(f.puts[0].payload.title, 'Current edits');
});

test('edits made while a create is in flight stay on screen and target the known ID on the next save', async () => {
  let release;
  const f = await filled({ create: () => new Promise(resolve => { release = resolve; }) });
  const saving = f.h.handleSaveDraft(); await flush();
  f.h.form.content = 'Changed while request pending'; release({ data: { id: 83 } }); await saving;
  assert.equal(f.redirects.length, 0);
  assert.equal(f.h.form.content, 'Changed while request pending');
  await f.h.handleSaveDraft(); assert.equal(f.posts.length, 1); assert.equal(f.puts[0].id, 83);
});

test('the latest list query keeps loading until it finishes even if an older query returns first', async () => {
  const pending = [];
  const f = harness({ list: params => new Promise(resolve => pending.push({ params, resolve })) }), h = f.list();
  const old = h.fetchArticles(); h.queryParams.page = 2; const recent = h.fetchArticles();
  pending[0].resolve({ data: { records: [{ id: 11 }], total: 30 } }); await old;
  assert.equal(h.loading.value, true);
  pending[1].resolve({ data: { records: [{ id: 22 }], total: 30 } }); await recent;
  assert.equal(h.loading.value, false);
  assert.deepEqual(clone(h.articles.value).map(row => row.id), [22]);
  assert.equal(pending[0].params.page, 1);
});

test('a late list response never replaces a newer page result', async () => {
  const pending = [];
  const f = harness({ list: () => new Promise(resolve => pending.push(resolve)) }), h = f.list();
  const old = h.fetchArticles(); h.queryParams.page = 2; const recent = h.fetchArticles();
  pending[1]({ data: { records: [{ id: 22 }], total: 40 } }); await recent;
  pending[0]({ data: { records: [{ id: 11 }], total: 30 } }); await old;
  assert.deepEqual(clone(h.articles.value).map(row => row.id), [22]); assert.equal(h.total.value, 40);
});

test('clearing a category and refreshing after an uncertain create preserves a usable recovery operation', async () => {
  const f = await filled({ create: async () => { throw timeout(); } });
  f.h.form.categoryId = undefined; await f.h.handleSaveDraft();
  const reload = harness({ session: f.session }), h = await reload.edit();
  assert.equal(h.canSave.value, true);
  assert.equal(h.form.title, 'Local draft');
  assert.equal(h.form.categoryId, null);
  await h.handleSaveDraft();
  assert.equal(reload.posts[0].key, f.posts[0].key);
  assert.equal(reload.posts[0].payload.categoryId, null);
});

test('legacy recovery records with omitted categoryId normalize to null without rotating the key', async () => {
  const f = await filled({ create: async () => { throw timeout(); } }); await f.h.handleSaveDraft();
  const storageKey = 'myblog:article-create:v1:id%3A7';
  const record = JSON.parse(f.session.getItem(storageKey));
  delete record.payload.categoryId; delete record.draft.categoryId;
  f.session.setItem(storageKey, JSON.stringify(record));
  const reload = harness({ session: f.session }), h = await reload.edit();
  assert.equal(h.canSave.value, true); assert.equal(h.form.categoryId, null);
  await h.handleSaveDraft(); assert.equal(reload.posts[0].key, record.key);
  assert.equal(reload.posts[0].payload.categoryId, null);
});

test('a late response merges only its ID into the reopened editor latest draft', async () => {
  let release;
  const old = await filled({ create: () => new Promise(resolve => { release = resolve; }) });
  const inFlight = old.h.handleSaveDraft(); await flush(); old.unmount();
  const reopened = harness({ session: old.session }), h = await reopened.edit();
  h.form.title = 'Reopened new input'; h.form.content = 'Reopened new body';
  release({ data: { id: 83 } }); await inFlight;
  const reload = harness({ session: old.session }), restored = await reload.edit();
  assert.equal(restored.form.title, 'Reopened new input');
  assert.equal(restored.form.content, 'Reopened new body');
  // Continued editing in the reopened instance must preserve the ID bound by the old response.
  h.form.content = 'Continued input after late response';
  const latest = harness({ session: old.session }), current = await latest.edit();
  assert.equal(current.form.content, 'Continued input after late response');
  await current.handleSaveDraft();
  assert.equal(latest.posts.length, 0); assert.equal(latest.puts[0].id, 83);
});

test('an inactive recovery session binds its ID but never chains an obsolete PUT', async () => {
  let attempts = 0, release;
  const old = await filled({ create: async () => { if (++attempts === 1) throw timeout(); return new Promise(resolve => { release = resolve; }); } });
  await old.h.handleSaveDraft(); old.h.form.title = 'Old recovery edits';
  const inFlight = old.h.handleSaveDraft(); await flush(); old.unmount();
  const reopened = harness({ session: old.session }), h = await reopened.edit(); h.form.title = 'Latest reopened edits';
  release({ data: { id: 83 } }); await inFlight;
  assert.equal(old.puts.length, 0);
  const reload = harness({ session: old.session }), restored = await reload.edit();
  assert.equal(restored.form.title, 'Latest reopened edits');
  await restored.handleSaveDraft(); assert.equal(reload.posts.length, 0); assert.equal(reload.puts[0].id, 83);
});

test('an old operation response never replaces a different operation recovery record', async () => {
  let release;
  const old = await filled({ create: () => new Promise(resolve => { release = resolve; }) });
  const inFlight = old.h.handleSaveDraft(); await flush(); old.unmount();
  const storageKey = 'myblog:article-create:v1:id%3A7';
  const record = JSON.parse(old.session.getItem(storageKey));
  record.key = '1553de9d-e060-408c-8c13-8148d1b679ba'; record.draft.title = 'Different operation';
  old.session.setItem(storageKey, JSON.stringify(record));
  release({ data: { id: 83 } }); await inFlight;
  const current = JSON.parse(old.session.getItem(storageKey));
  assert.equal(current.key, record.key); assert.equal(current.draft.title, 'Different operation');
  assert.equal(current.articleId, undefined);
});
