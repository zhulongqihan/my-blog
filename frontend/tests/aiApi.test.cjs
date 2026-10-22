const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');
const source = ts.transpileModule(fs.readFileSync(path.join(__dirname, '../src/services/aiApi.ts'), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText;
const requestId = 'ai-11111111-1111-4111-8111-111111111111';

function client(fetch) {
    const module = { exports: {} };
    let fireTimeout;
    vm.runInNewContext(source, {
        module, exports: module.exports, fetch, AbortController, DOMException, TextDecoder,
        window: { setTimeout: callback => { fireTimeout = callback; return setTimeout(callback, 150000); }, clearTimeout },
        console
    });
    return { api: module.exports, timeout: () => fireTimeout() };
}
function stream(chunks, cancel) {
    return new Response(new ReadableStream({
        start(controller) {
            for (const text of chunks) controller.enqueue(typeof text === 'string' ? new TextEncoder().encode(text) : text);
            controller.close();
        }, cancel
    }), { headers: { 'Content-Type': 'text/event-stream' } });
}
function callbacks() {
    const events = { chunks: [], ends: 0, errors: [], meta: [] };
    let resolve;
    const finished = new Promise(done => { resolve = done; });
    return { events, finished, options: {
        onChunk: text => events.chunks.push(text),
        onEnd: () => { events.ends++; resolve(); },
        onError: (message, details) => { events.errors.push({ message, details }); resolve(); },
        onMeta: meta => events.meta.push(meta)
    } };
}
const tick = () => new Promise(resolve => setImmediate(resolve));

test('stop finish_reason plus repeated DONE emits one terminal callback', async () => {
    const c = client(async () => stream([
        `event: meta\ndata: {"requestId":"${requestId}","sources":[]}\n\n`,
        'data: {"choices":[{"delta":{"content":"hello"},"finish_reason":"stop"}]}\n\n',
        'data: [DONE]\n\ndata: [DONE]\n\n'
    ]));
    const call = callbacks(); c.api.sendChatMessage({ query: 'test' }, call.options);
    await call.finished; await tick();
    assert.deepEqual(call.events.chunks, ['hello']);
    assert.equal(call.events.ends, 1); assert.equal(call.events.errors.length, 0);
    assert.equal(call.events.meta[0].requestId, requestId);
});

test('split UTF8, CRLF and final frame without newline survive', async () => {
    const bytes = new TextEncoder().encode('data: {"choices":[{"delta":{"content":"你好"}}]}\r\n\r\ndata: [DONE]');
    const c = client(async () => stream(Array.from(bytes, byte => new Uint8Array([byte]))));
    const call = callbacks(); c.api.sendChatMessage({ query: 'test' }, call.options);
    await call.finished; await tick();
    assert.deepEqual(call.events.chunks, ['你好']); assert.equal(call.events.ends, 1);
});

test('provider error is mapped locally, correlated and never followed by end', async () => {
    const c = client(async () => stream([
        `event: error\ndata: {"code":"AI_PROVIDER_UNAVAILABLE","message":"sk-secret internal-url","requestId":"${requestId}"}\n\n`,
        'data: [DONE]\n\n'
    ]));
    const call = callbacks(); c.api.sendChatMessage({ query: 'test' }, call.options);
    await call.finished; await tick();
    assert.equal(call.events.ends, 0); assert.equal(call.events.errors.length, 1);
    assert.equal(call.events.errors[0].details.requestId, requestId);
    assert.doesNotMatch(call.events.errors[0].message, /sk-secret|internal-url/);
});

test('premature EOF is an error rather than a successful completion', async () => {
    const c = client(async () => stream(['data: {"choices":[{"delta":{"content":"partial"},"finish_reason":"stop"}]}\n\n']));
    const call = callbacks(); c.api.sendChatMessage({ query: 'test' }, call.options);
    await call.finished; await tick();
    assert.equal(call.events.ends, 0);
    assert.equal(call.events.errors[0].details.code, 'AI_STREAM_INCOMPLETE');
});

test('abort cancels a blocked reader without stale callbacks', async () => {
    let cancellations = 0;
    const c = client(async () => new Response(new ReadableStream({ cancel() { cancellations++; } })));
    const call = callbacks(); const controller = c.api.sendChatMessage({ query: 'test' }, call.options);
    await tick(); controller.abort(); await tick();
    assert.equal(cancellations, 1); assert.equal(call.events.ends, 0); assert.equal(call.events.errors.length, 0);
});

test('503 preserves stable overload code and request ID', async () => {
    const c = client(async () => new Response(JSON.stringify({ code: 'AI_GATEWAY_BUSY', message: 'ignored', requestId }), { status: 503 }));
    const call = callbacks(); c.api.sendChatMessage({ query: 'test' }, call.options);
    await call.finished; await tick();
    assert.equal(call.events.errors[0].details.code, 'AI_GATEWAY_BUSY');
    assert.equal(call.events.errors[0].details.requestId, requestId);
});

test('client deadline cancels pending IO and reports timeout once', async () => {
    let cancellations = 0;
    const c = client(async () => new Response(new ReadableStream({ cancel() { cancellations++; } })));
    const call = callbacks(); c.api.sendChatMessage({ query: 'test' }, call.options);
    await tick(); c.timeout(); await call.finished; await tick();
    assert.equal(cancellations, 1); assert.equal(call.events.errors.length, 1);
    assert.equal(call.events.errors[0].details.code, 'AI_TIMEOUT'); assert.equal(call.events.ends, 0);
});
