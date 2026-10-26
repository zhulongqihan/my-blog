const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');

test('create requests carry the stable Idempotency-Key header without changing the global timeout budget', async () => {
  const calls = [], module = { exports: {} };
  const source = ts.transpileModule(fs.readFileSync(path.join(__dirname, '../src/api/article.ts'), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true },
  }).outputText;
  vm.runInNewContext(source, {
    module, exports: module.exports,
    require: name => {
      assert.equal(name, '@/utils/request');
      return { post: async (...args) => { calls.push(args); return { data: { id: 83 } }; } };
    },
  });
  const payload = { title: 'Contract fixture' }, key = '1553de9d-e060-408c-8c13-8148d1b679ba';
  await module.exports.createArticle(payload, key);
  assert.equal(calls[0][0], '/admin/articles');
  assert.equal(calls[0][1], payload);
  assert.equal(calls[0][2].headers['Idempotency-Key'], key);
  assert.equal(calls[0][2].timeout, undefined);
  await module.exports.createArticle(payload);
  assert.equal(calls[1][2], undefined);
});
