async (page) => {
  // Run with playwright-cli run-code --filename tests/article-save.browser.js against a local production preview.
  // All API traffic, identities and writes are fixtures. Nothing may reach a real backend.
  const base = (typeof process !== 'undefined' && process.env.APP_BASE_URL) || 'http://127.0.0.1:5184';
  const origin = new URL(base).origin;
  if (new URL(origin).hostname !== '127.0.0.1') throw new Error('Local preview required');
  const results = [], writes = [], created = new Map();
  const check = (name, pass, details = {}) => results.push({ name, pass: !!pass, ...details });
  const gate = () => { let release; const promise = new Promise(resolve => { release = resolve; }); return { promise, release }; };
  const firstCreate = gate(), oldList = gate(), newList = gate();
  const until = async (predicate, timeout = 4000) => {
    const start = Date.now();
    while (Date.now() - start < timeout) { if (await predicate()) return true; await page.waitForTimeout(25); }
    return false;
  };
  const ok = data => ({ code: 200, message: 'ok', data });
  let postAttempts = 0, putAttempts = 0, listRace = false;
  const listRequests = [];
  await page.unroute('**/*');
  await page.route('**/*', async route => {
    const request = route.request(), url = new URL(request.url());
    if (url.origin !== origin || url.pathname.startsWith('/ws')) return route.abort('blockedbyclient');
    if (!url.pathname.startsWith('/api/')) return route.continue();
    if (request.method() === 'POST' && url.pathname.endsWith('/admin/articles')) {
      postAttempts++;
      const payload = request.postDataJSON(), key = request.headers()['idempotency-key'];
      writes.push({ method: 'POST', key, payload });
      if (!created.has(key)) created.set(key, 82 + created.size + 1);
      const id = created.get(key);
      if (postAttempts === 1) {
        await firstCreate.promise; // Simulate a committed database write whose response misses the 15s client budget.
        try { return await route.fulfill({ json: ok({ id, ...payload }) }); } catch { return; }
      }
      return route.fulfill({ json: ok({ id, ...payload }) });
    }
    if (request.method() === 'PUT' && /\/admin\/articles\/\d+$/.test(url.pathname)) {
      putAttempts++;
      writes.push({ method: 'PUT', path: url.pathname, payload: request.postDataJSON() });
      if (putAttempts === 1) return route.fulfill({ status: 500, json: { code: 500, message: 'Controlled lost update result' } });
      return route.fulfill({ json: ok({ id: Number(url.pathname.split('/').pop()) }) });
    }
    if (request.method() !== 'GET') return route.fulfill({ status: 409, json: { code: 409, message: 'Unexpected fixture write blocked' } });
    if (url.pathname.endsWith('/auth/me')) return route.fulfill({ json: ok({ id: 7, username: 'local-save-fixture', nickname: '本地保存夹具', role: 'ADMIN' }) });
    if (url.pathname.endsWith('/admin/categories')) return route.fulfill({ json: ok([{ id: 7, name: '分类清空夹具' }]) });
    if (url.pathname.endsWith('/admin/tags')) return route.fulfill({ json: ok([]) });
    if (url.pathname.endsWith('/admin/notifications/unread-count')) return route.fulfill({ json: ok(0) });
    if (url.pathname.endsWith('/admin/articles')) {
      const current = Number(url.searchParams.get('page') || 1);
      listRequests.push(current);
      if (listRace && current === 2) await oldList.promise;
      if (listRace && current === 3) await newList.promise;
      return route.fulfill({ json: ok({ records: [{ id: current * 100, title: `夹具第${current}页`, tagNames: [], published: false, featured: false }], total: 40 }) });
    }
    return route.fulfill({ json: ok({ records: [], total: 0 }) });
  });
  if (typeof page.routeWebSocket === 'function') await page.routeWebSocket('**/*', socket => socket.close());
  await page.addInitScript(() => {
    localStorage.setItem('admin_token', 'local-fixture-token-never-valid');
    localStorage.setItem('admin_user', JSON.stringify({ id: 7, username: 'local-save-fixture', role: 'ADMIN' }));
  });
  const navigate = path => page.goto(`${origin}/admin/${path}`, { waitUntil: 'domcontentloaded' });
  const title = () => page.getByPlaceholder('请输入文章标题');
  const editor = () => page.locator('.md-editor .cm-content').first();
  const save = () => page.getByRole('button', { name: '存为草稿', exact: true });
  const notice = () => page.locator('.save-notice');
  try {
    await navigate('articles/create');
    await page.evaluate(() => sessionStorage.clear());
    await page.reload({ waitUntil: 'domcontentloaded' });
    await save().waitFor({ state: 'visible' }); await until(async () => !(await save().isDisabled()));
    await title().fill('本地同标题草稿'); await editor().fill('# 原始内容');
    const categorySelect = page.locator('.article-edit .el-select').first();
    await categorySelect.click(); await page.getByRole('option', { name: '分类清空夹具', exact: true }).click();
    await categorySelect.hover(); await categorySelect.locator('.el-select__clear').click();
    check('category-can-be-selected-and-cleared', !(await categorySelect.innerText()).includes('分类清空夹具'));
    await save().evaluate(button => { button.click(); button.click(); });
    check('concurrent-click-only-one-post', await until(() => postAttempts === 1) && postAttempts === 1);
    check('in-flight-button-has-native-disabled-and-loading', await save().isDisabled() && await save().getAttribute('class').then(value => value.includes('is-loading')));
    check('first-post-has-uuid-header', /^[0-9a-f-]{36}$/i.test(writes[0]?.key));
    check('cleared-category-is-submitted-as-null', writes[0]?.payload.categoryId === null);
    await notice().filter({ hasText: '待确认' }).waitFor({ state: 'visible', timeout: 19000 });
    check('real-15-second-timeout-keeps-create-input', await title().inputValue() === '本地同标题草稿' && (await editor().innerText()).includes('原始内容'));
    check('timeout-presents-unknown-result', (await notice().innerText()).includes('文章可能已保存'));
    firstCreate.release();
    await title().fill('本地修改后草稿'); await editor().fill('# 超时后用户的新内容');
    await page.reload({ waitUntil: 'domcontentloaded' });
    check('refresh-restores-current-title', await until(async () => await title().inputValue() === '本地修改后草稿'));
    check('refresh-restores-current-body', (await editor().innerText()).includes('超时后用户的新内容'));
    check('cleared-category-remains-recoverable-after-timeout-and-refresh', await until(async () => !(await save().isDisabled())) && !(await categorySelect.innerText()).includes('分类清空夹具'));
    await until(async () => !(await save().isDisabled())); await save().click();
    check('recovery-post-then-put-observed', await until(() => postAttempts === 2 && putAttempts === 1));
    check('recovery-preserves-first-key-and-payload', writes[1]?.key === writes[0]?.key && JSON.stringify(writes[1]?.payload) === JSON.stringify(writes[0]?.payload));
    check('server-fixture-created-one-article', created.size === 1);
    check('current-edits-put-to-original-id', writes[2]?.path === '/api/admin/articles/83' && writes[2]?.payload.title === '本地修改后草稿' && writes[2]?.payload.content === '# 超时后用户的新内容');
    check('failed-put-keeps-input-and-known-id-notice', await until(async () => (await notice().innerText()).includes('更新同一篇文章')) && await title().inputValue() === '本地修改后草稿');
    await page.reload({ waitUntil: 'domcontentloaded' });
    await until(async () => !(await save().isDisabled())); await save().click();
    check('reloaded-known-id-uses-only-put', await until(() => putAttempts === 2) && postAttempts === 2);
    check('successful-save-returns-to-list', await until(() => new URL(page.url()).pathname === '/admin/articles'));
    check('successful-navigation-clears-recovery', await page.evaluate(() => Object.keys(sessionStorage).every(key => !key.startsWith('myblog:article-create:'))));
    await navigate('articles/create'); await until(async () => !(await save().isDisabled()));
    await title().fill('本地同标题草稿'); await editor().fill('# 独立新文章'); await save().click();
    check('independent-same-title-article-has-new-key', await until(() => postAttempts === 3) && writes.filter(write => write.method === 'POST')[2]?.key !== writes[0]?.key && created.size === 2);
    await until(() => new URL(page.url()).pathname === '/admin/articles');
    await page.getByText('夹具第1页', { exact: true }).waitFor();
    listRace = true;
    await page.locator('.el-pager li.number').filter({ hasText: /^2$/ }).click();
    await until(() => listRequests.includes(2));
    await page.locator('.el-pager li.number').filter({ hasText: /^3$/ }).click();
    await until(() => listRequests.includes(3));
    newList.release();
    check('latest-list-page-shown', await until(() => page.getByText('夹具第3页', { exact: true }).isVisible()));
    const oldResponse = page.waitForResponse(response => new URL(response.url()).searchParams.get('page') === '2');
    oldList.release(); await (await oldResponse).finished();
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
    check('late-list-result-cannot-replace-latest-page', await page.getByText('夹具第3页', { exact: true }).isVisible() && await page.getByText('夹具第2页', { exact: true }).count() === 0);
  } finally {
    firstCreate.release(); oldList.release(); newList.release();
  }
  const report = { origin, results, writes, createdCount: created.size, failures: results.filter(item => !item.pass).map(item => item.name) };
  await page.evaluate(value => { window.__articleSaveRegression = value; }, report);
  if (report.failures.length) throw new Error(JSON.stringify(report));
  return report;
}
