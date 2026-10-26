async (page) => {
  // Local-only browser regression. Every API request and every save is fulfilled by this file.
  const base = (typeof process !== 'undefined' && process.env.APP_BASE_URL) || 'http://127.0.0.1:5176';
  const origin = new URL(base).origin;
  if (new URL(origin).hostname !== '127.0.0.1') throw new Error('Safety guard: APP_BASE_URL must use 127.0.0.1');
  const results = [];
  const requests = [];
  const writes = [];
  const gate = () => {
    let release;
    const promise = new Promise(resolve => { release = resolve; });
    return { promise, release };
  };
  const check = (name, ok, details = {}) => results.push({ name, pass: !!ok, ...details });
  const until = async (predicate, timeout = 3000) => {
    const start = Date.now();
    while (Date.now() - start < timeout) {
      if (await predicate()) return true;
      await page.waitForTimeout(25);
    }
    return false;
  };
  const ok = data => ({ code: 200, message: 'ok', data });
  const category = { id: 7, name: '边界回归分类' };
  const tag = { id: 9, name: '边界回归标签' };
  const fixture = id => ({
    id, title: `本地文章${id}`, summary: '原摘要', content: `# 本地正文${id}\n\n原始内容`,
    coverImage: '', category, tags: [tag], published: true, featured: true,
  });
  let scenario = 'metadata-failure';
  let categoryAttempts = 0;
  let detailAttempts = 0;
  const categoryGate = gate();
  const oldArticleGate = gate();
  let oldArticleRequested = false;
  await page.unroute('**/*');
  await page.route('**/*', async route => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.origin !== origin || url.pathname.startsWith('/ws')) return route.abort('blockedbyclient');
    if (!url.pathname.startsWith('/api/')) return route.continue();
    const record = { scenario, method: request.method(), path: url.pathname };
    requests.push(record);
    if (request.method() !== 'GET') {
      // Mocked writes let the test inspect payloads without reaching a server.
      writes.push({ ...record, payload: request.postDataJSON() });
      return route.fulfill({ json: ok({ ...fixture(82), ...request.postDataJSON() }) });
    }
    if (url.pathname.endsWith('/admin/categories')) {
      categoryAttempts++;
      if (scenario === 'metadata-failure' && categoryAttempts === 1) {
        await categoryGate.promise;
        return route.fulfill({ status: 500, json: { code: 500, message: 'Local controlled category failure' } });
      }
      return route.fulfill({ json: ok([category]) });
    }
    if (url.pathname.endsWith('/admin/tags')) return route.fulfill({ json: ok([tag]) });
    if (/\/admin\/articles\/\d+$/.test(url.pathname)) {
      const id = Number(url.pathname.split('/').pop());
      detailAttempts++;
      if (scenario === 'detail-failure' && detailAttempts === 1) {
        return route.fulfill({ status: 500, json: { code: 500, message: 'Local controlled article failure' } });
      }
      if (scenario === 'route-race' && id === 82) {
        oldArticleRequested = true;
        await oldArticleGate.promise;
      }
      return route.fulfill({ json: ok(fixture(id)) });
    }
    if (url.pathname.endsWith('/admin/notifications/unread-count')) return route.fulfill({ json: ok(0) });
    if (url.pathname.endsWith('/auth/me')) return route.fulfill({ json: ok({ username: 'local-regression', role: 'ADMIN' }) });
    return route.fulfill({ json: ok({ content: [], records: [], totalElements: 0, total: 0 }) });
  });
  if (typeof page.routeWebSocket === 'function') await page.routeWebSocket('**/*', socket => socket.close());
  await page.addInitScript(() => {
    window.global = window;
    localStorage.setItem('admin_token', 'local-regression-fake-token-never-valid');
    localStorage.setItem('admin_user', JSON.stringify({ username: 'local-regression', nickname: '本地回归', role: 'ADMIN' }));
  });
  const navigate = path => page.goto(`${origin}/admin/${path}`, { waitUntil: 'domcontentloaded' });
  const title = () => page.getByPlaceholder('请输入文章标题');
  const editor = () => page.locator('.md-editor .cm-content').first();
  const save = () => page.getByRole('button', { name: '存为草稿', exact: true });
  try {
    await navigate('articles/edit/82');
    await page.getByRole('heading', { name: '编辑文章' }).waitFor({ state: 'visible', timeout: 15000 });
    check('single-slow-category-does-not-block-article', await until(async () => await title().inputValue() === fixture(82).title));
    check('single-slow-category-save-is-disabled', await save().isDisabled());
    const editedTitle = '用户在分类重试前修改的标题';
    const editedContent = '# 用户修改的正文\n\n这段编辑必须在选项重试后保留。';
    await title().fill(editedTitle);
    await editor().fill(editedContent);
    categoryGate.release();
    check('metadata-failure-shows-retry', await until(() => page.getByRole('button', { name: '重新加载分类和标签', exact: true }).isVisible()));
    check('metadata-failure-keeps-edited-title', await title().inputValue() === editedTitle);
    check('metadata-failure-keeps-edited-body', (await editor().innerText()).includes('用户修改的正文'));
    check('metadata-failure-save-is-disabled', await save().isDisabled());
    await page.getByRole('button', { name: '重新加载分类和标签', exact: true }).click();
    check('metadata-retry-enables-save', await until(async () => !(await save().isDisabled())));
    check('metadata-retry-keeps-edited-title', await title().inputValue() === editedTitle);
    check('metadata-retry-keeps-edited-body', (await editor().innerText()).includes('这段编辑必须在选项重试后保留。'));
    check('metadata-retry-keeps-category-and-tag', await page.locator('.article-edit .el-select').first().innerText().then(text => text.includes(category.name))
      && await page.locator('.article-edit .el-select').nth(1).innerText().then(text => text.includes(tag.name)));
    await save().click();
    check('draft-save-is-only-a-mocked-put', await until(() => writes.some(write => write.method === 'PUT' && write.path.endsWith('/admin/articles/82'))));
    const draftPayload = writes.find(write => write.method === 'PUT')?.payload;
    check('draft-save-preserves-relation-and-status-payload', draftPayload?.categoryId === category.id && JSON.stringify(draftPayload.tagIds) === '[9]' && draftPayload.published === false && draftPayload.featured === true, { payload: draftPayload });
    check('draft-save-keeps-user-edits', draftPayload?.title === editedTitle && draftPayload?.content === editedContent);

    scenario = 'detail-failure';
    detailAttempts = 0;
    await navigate('articles/edit/82');
    const retryArticle = page.getByRole('button', { name: '重新加载文章', exact: true });
    check('article-failure-shows-explicit-retry', await until(() => retryArticle.isVisible()));
    check('article-failure-save-is-disabled', await save().isDisabled());
    check('article-failure-has-readable-alert', await page.getByRole('alert').filter({ hasText: '文章加载失败' }).isVisible());
    await retryArticle.click();
    check('article-retry-hydrates-existing-article', await until(async () => await title().inputValue() === fixture(82).title));
    check('article-retry-enables-save', await until(async () => !(await save().isDisabled())));

    scenario = 'route-race';
    await navigate('articles/edit/82');
    check('race-old-request-started', await until(() => oldArticleRequested));
    // A same-app click exercises Vue Router's reused ArticleEdit component, unlike a full reload.
    await page.evaluate(() => {
      const root = document.querySelector('#app');
      const app = root && root.__vue_app__;
      app.config.globalProperties.$router.push('/articles/edit/78');
    });
    check('race-new-article-hydrates-first', await until(async () => await title().inputValue() === fixture(78).title));
    const oldResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/articles/82');
    oldArticleGate.release();
    await (await oldResponse).finished();
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
    check('late-old-response-does-not-overwrite-new-article', await title().inputValue() === fixture(78).title && (await editor().innerText()).includes('本地正文78'));

    scenario = 'create-publish';
    await navigate('articles/create');
    await page.getByRole('heading', { name: '新建文章' }).waitFor({ state: 'visible' });
    await until(async () => !(await save().isDisabled()));
    await title().fill('本地新建发布假文章');
    await editor().fill('# 新建假正文');
    await page.locator('.article-edit .el-select').first().click();
    await page.getByRole('option', { name: category.name, exact: true }).click();
    await page.locator('.article-edit .el-select').nth(1).click();
    await page.getByRole('option', { name: tag.name, exact: true }).click();
    await page.keyboard.press('Escape');
    await page.getByRole('button', { name: '发布文章', exact: true }).click();
    check('create-publish-is-only-a-mocked-post', await until(() => writes.some(write => write.method === 'POST' && write.path.endsWith('/admin/articles'))));
    const publishPayload = writes.find(write => write.method === 'POST')?.payload;
    check('create-publish-keeps-relation-and-status-payload', publishPayload?.categoryId === category.id && JSON.stringify(publishPayload.tagIds) === '[9]' && publishPayload.published === true, { payload: publishPayload });
    check('create-publish-does-not-fetch-detail', !requests.some(request => request.scenario === 'create-publish' && /\/admin\/articles\/\d+$/.test(request.path)));
  } finally {
    categoryGate.release();
    oldArticleGate.release();
  }
  const report = { origin, results, requests, writes, failures: results.filter(result => !result.pass).map(result => result.name) };
  await page.evaluate(report => { window.__adminLoadingEdges = report; }, report);
  if (report.failures.length) throw new Error('Admin loading edges failed: ' + report.failures.join(', '));
  return report;
}
