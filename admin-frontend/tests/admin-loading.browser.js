async (page) => {
  // Run with: playwright-cli --session admin-loading-tests run-code --filename admin-frontend/tests/admin-loading.browser.js
  // All API traffic is mocked. APP_BASE_URL, when provided, must stay on 127.0.0.1.
  const base = (typeof process !== 'undefined' && process.env.APP_BASE_URL) || 'http://127.0.0.1:5176';
  const app = new URL(base);
  if (app.hostname !== '127.0.0.1' || !['http:', 'https:'].includes(app.protocol)) {
    throw new Error('Safety guard: APP_BASE_URL must use 127.0.0.1');
  }
  const results = [];
  const requests = [];
  const warnings = [];
  const blockedWrites = [];
  const gate = () => {
    let release;
    const promise = new Promise(resolve => { release = resolve; });
    return { promise, release };
  };
  const check = (name, ok, details = {}) => {
    results.push({ name, pass: !!ok, ...details });
  };
  const waitUntil = async (predicate, timeout = 2500) => {
    const started = Date.now();
    while (Date.now() - started < timeout) {
      if (await predicate()) return true;
      await page.waitForTimeout(25);
    }
    return false;
  };
  const response = data => ({ code: 200, message: 'ok', data });
  const categories = [{ id: 7, name: '本地回归分类' }];
  const tags = [{ id: 9, name: '本地回归标签' }];
  const article = {
    id: 82, title: '本地假文章：正文优先回显', summary: '本地假摘要',
    content: '# 本地正文\n\n接口均由浏览器拦截。', coverImage: '',
    category: categories[0], tags, published: true, featured: false,
  };
  const stats = {
    totalArticles: 29, totalComments: 8, totalViews: 123, totalUsers: 4,
    todayArticles: 1, todayComments: 2, todayViews: 3, todayUsers: 1,
    viewTrend: [], articleTrend: [], categoryStats: [], popularArticles: [], recentComments: [],
  };
  let scenario = 'edit';
  let metadataGate = gate();
  let articleGate = gate();
  let dashboardGate = gate();
  let dashboardAttempts = 0;
  const consoleListener = msg => {
    if (msg.type() === 'warning' || msg.type() === 'error') warnings.push(msg.text());
  };
  page.on('console', consoleListener);
  await page.unroute('**/*');
  await page.route('**/*', async route => {
    const request = route.request();
    const url = new URL(request.url());
    // The only requests allowed through are local Vite assets. No real API or remote request can escape.
    if (url.origin !== app.origin) return route.abort('blockedbyclient');
    if (url.pathname.startsWith('/ws')) return route.abort('blockedbyclient');
    if (!url.pathname.startsWith('/api/') && !url.pathname.startsWith('/ws')) return route.continue();
    requests.push({ scenario, method: request.method(), path: url.pathname });
    if (request.method() !== 'GET') {
      blockedWrites.push({ method: request.method(), path: url.pathname });
      return route.fulfill({ status: 409, json: { code: 409, message: 'Local regression blocks all writes' } });
    }
    if (url.pathname.endsWith('/admin/categories') || url.pathname.endsWith('/admin/tags')) {
      await metadataGate.promise;
      return route.fulfill({ json: response(url.pathname.endsWith('/categories') ? categories : tags) });
    }
    if (/\/admin\/articles\/\d+$/.test(url.pathname)) {
      await articleGate.promise;
      return route.fulfill({ json: response(article) });
    }
    if (url.pathname.endsWith('/admin/dashboard/stats')) {
      dashboardAttempts += 1;
      await dashboardGate.promise;
      if (dashboardAttempts === 1) return route.fulfill({ status: 500, json: { code: 500, message: 'Local controlled stats failure' } });
      return route.fulfill({ json: response(stats) });
    }
    if (url.pathname.endsWith('/auth/me')) {
      return route.fulfill({ json: response({ username: 'local-regression', nickname: '本地回归', role: 'ADMIN' }) });
    }
    if (url.pathname.endsWith('/admin/notifications/unread-count')) return route.fulfill({ json: response(0) });
    return route.fulfill({ json: response({ content: [], records: [], totalElements: 0, total: 0 }) });
  });
  if (typeof page.routeWebSocket === 'function') {
    await page.routeWebSocket('**/*', socket => socket.close());
  }
  await page.addInitScript(() => {
    // SockJS's development bundle expects Node's global alias; production does not need this harness shim.
    window.global = window;
    localStorage.setItem('admin_token', 'local-regression-fake-token-never-valid');
    localStorage.setItem('admin_user', JSON.stringify({ username: 'local-regression', nickname: '本地回归', role: 'ADMIN' }));
  });
  const navigate = path => page.goto(new URL(`/admin/${path}`, app.origin).href, { waitUntil: 'domcontentloaded' });
  try {
    await navigate('articles/edit/82');
    await page.getByRole('heading', { name: '编辑文章' }).waitFor({ state: 'visible', timeout: 15000 });
    check('edit-metadata-requests-start-in-parallel', await waitUntil(() => requests.filter(r => r.scenario === 'edit' && /\/admin\/(categories|tags)$/.test(r.path)).length === 2));
    const detailBeforeMetadata = await waitUntil(() => requests.some(r => r.scenario === 'edit' && /\/admin\/articles\/82$/.test(r.path)), 750);
    check('edit-detail-starts-before-metadata-finishes', detailBeforeMetadata);
    const loadingVisible = await page.locator('.article-edit .el-loading-mask').first().isVisible().catch(() => false);
    check('edit-has-visible-initial-loading', loadingVisible);
    check('edit-save-disabled-while-loading', await page.getByRole('button', { name: '存为草稿', exact: true }).isDisabled());
    check('edit-publish-disabled-while-loading', await page.getByRole('button', { name: '发布文章', exact: true }).isDisabled());
    articleGate.release();
    const title = page.getByPlaceholder('请输入文章标题');
    const titleBeforeMetadata = await waitUntil(async () => (await title.inputValue()) === article.title, 1000);
    check('edit-content-visible-before-metadata-finishes', titleBeforeMetadata);
    const markdownVisible = await page.locator('.md-editor .cm-content').first().isVisible().catch(() => false);
    const markdownText = markdownVisible ? await page.locator('.md-editor .cm-content').first().innerText() : '';
    check('edit-markdown-visible-before-metadata-finishes', markdownVisible && markdownText.includes('本地正文') && markdownText.includes('接口均由浏览器拦截。'));
    check('edit-save-stays-disabled-until-metadata-ready', await page.getByRole('button', { name: '存为草稿', exact: true }).isDisabled());
    metadataGate.release();
    check('edit-eventually-hydrates-fixture', await waitUntil(async () => (await title.inputValue()) === article.title));
    check('edit-save-enabled-after-all-data-ready', await waitUntil(async () => !(await page.getByRole('button', { name: '存为草稿', exact: true }).isDisabled())));

    scenario = 'create';
    metadataGate = gate();
    await navigate('articles/create');
    await page.getByRole('heading', { name: '新建文章' }).waitFor({ state: 'visible' });
    await waitUntil(() => requests.filter(r => r.scenario === 'create' && /\/admin\/(categories|tags)$/.test(r.path)).length === 2);
    check('create-does-not-request-an-article-detail', !requests.some(r => r.scenario === 'create' && /\/admin\/articles\//.test(r.path)));
    check('create-save-disabled-until-metadata-ready', await page.getByRole('button', { name: '存为草稿', exact: true }).isDisabled());
    metadataGate.release();
    check('create-save-enabled-after-metadata-ready', await waitUntil(async () => !(await page.getByRole('button', { name: '存为草稿', exact: true }).isDisabled())));

    scenario = 'dashboard';
    await navigate('dashboard');
    await waitUntil(() => dashboardAttempts === 1);
    const values = await page.locator('.stat-value').allTextContents();
    check('dashboard-pending-does-not-display-fake-zero', !values.some(value => value.trim() === '0'), { observedValues: values });
    const dashboardLoading = await page.locator('.dashboard .el-loading-mask, .dashboard .el-skeleton, .dashboard[aria-busy="true"]').first().isVisible().catch(() => false)
      || await page.getByText(/加载统计|加载中|正在加载/).first().isVisible().catch(() => false);
    check('dashboard-pending-has-visible-loading', dashboardLoading);
    dashboardGate.release();
    const retry = page.getByRole('button', { name: /重试|重新加载/ }).first();
    const retryVisible = await waitUntil(() => retry.isVisible(), 2000);
    check('dashboard-failure-offers-retry', retryVisible);
    if (retryVisible) {
      await retry.click();
      check('dashboard-retry-loads-real-count', await waitUntil(async () => (await page.locator('.stat-value').allTextContents()).some(v => v.trim() === '29')));
    }
    check('loading-directive-is-registered', !warnings.some(message => /Failed to resolve directive:\s*loading/.test(message)), {
      loadingWarnings: warnings.filter(message => /Failed to resolve directive:\s*loading/.test(message)),
    });
    check('no-real-write-was-attempted', blockedWrites.length === 0, { blockedWrites });
  } finally {
    metadataGate.release();
    articleGate.release();
    dashboardGate.release();
    page.off('console', consoleListener);
  }
  const report = { base: app.origin, results, requests, failures: results.filter(result => !result.pass).map(result => result.name) };
  await page.evaluate(report => { window.__adminLoadingRegression = report; }, report);
  console.log('ADMIN_LOADING_REGRESSION ' + JSON.stringify(report, null, 2));
  if (report.failures.length) throw new Error('Admin loading regression failed: ' + report.failures.join(', '));
  return report;
}
