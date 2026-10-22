/** AI Gateway 的 SSE 流式客户端。浏览器只访问同源 Backend，不持有模型密钥。 */

// Blog AI always goes through the same-origin Gateway; provider credentials stay server-side.
const AI_API_URL = '/api/ai';

interface ChatCompletionChunk {
  choices: Array<{
    delta: { content?: string };
    finish_reason: string | null;
  }>;
}

export interface AiStreamMeta {
  requestId: string;
  model: string;
  mode: string;
  sources: Array<{ articleId: number; title: string; score: number }>;
}

export interface AiStreamError {
  code: string;
  message: string;
  requestId?: string;
}

export interface StreamCallbacks {
  onChunk: (text: string) => void;
  onEnd: () => void;
  onError: (error: string, details?: AiStreamError) => void;
  onMeta?: (meta: AiStreamMeta) => void;
}

export interface ChatRequest {
  systemPrompt?: string;
  query: string;
  articleId?: number;
  mode?: string;
}

export function getUserId(): string {
  const KEY = 'ai-user-id';
  let id = localStorage.getItem(KEY);
  if (!id) {
    const uuid = globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
    id = `blog-visitor-${uuid}`;
    localStorage.setItem(KEY, id);
  }
  return id;
}

export function sendChatMessage(request: ChatRequest, callbacks: StreamCallbacks): AbortController {
  const controller = new AbortController();
  let reader: ReadableStreamDefaultReader<Uint8Array> | undefined;
  let settled = false;
  let requestId: string | undefined;
  const messages: Record<string, string> = {
    AI_GATEWAY_BUSY: 'AI 服务繁忙，请稍后重试',
    AI_PROVIDER_BUSY: 'AI 服务繁忙，请稍后重试',
    AI_TIMEOUT: 'AI 请求超时，请稍后重试',
    AI_CONTEXT_UNAVAILABLE: '文章上下文暂时不可用，请稍后重试',
    AI_STREAM_INCOMPLETE: 'AI 响应中断，请重试',
    AI_CONNECTION_FAILED: '网络连接失败，请稍后重试',
  };
  const fail = (code: string, correlationId = requestId) => {
    if (settled) return;
    settled = true;
    const message = messages[code] || 'AI 服务暂时不可用，请稍后重试';
    callbacks.onError(correlationId ? `${message}（请求 ID：${correlationId}）` : message,
      { code, message, requestId: correlationId });
  };
  const providerError = (value: unknown) => {
    const error = value as Partial<AiStreamError> | null;
    const code = typeof error?.code === 'string' && /^AI_[A-Z_]+$/.test(error.code)
      ? error.code : 'AI_PROVIDER_UNAVAILABLE';
    const id = typeof error?.requestId === 'string' && /^ai-[a-f0-9-]{36}$/.test(error.requestId)
      ? error.requestId : requestId;
    fail(code, id);
  };
  controller.signal.addEventListener('abort', () => {
    settled = true;
    void reader?.cancel().catch(() => undefined);
  }, { once: true });
  const timeout = window.setTimeout(() => {
    fail('AI_TIMEOUT');
    controller.abort();
  }, 150_000);

  const fetchStream = async () => {
    try {
      const response = await fetch(`${AI_API_URL}/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
        signal: controller.signal,
      });
      if (!response.ok) {
        const error = await response.json().catch(() => null);
        if (error?.code) providerError(error);
        else fail(response.status === 429 || response.status === 503 ? 'AI_GATEWAY_BUSY' : 'AI_PROVIDER_UNAVAILABLE');
        return;
      }
      if (!response.body) { fail('AI_STREAM_INCOMPLETE'); return; }
      reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';
      let eventName = '';
      let dataLines: string[] = [];
      const flushEvent = () => {
        const name = eventName;
        eventName = '';
        if (dataLines.length === 0 || settled) { dataLines = []; return; }
        const data = dataLines.join('\n');
        dataLines = [];
        if (name === 'meta') {
          try {
            const meta = JSON.parse(data) as AiStreamMeta;
            requestId = meta.requestId;
            callbacks.onMeta?.(meta);
          } catch { /* metadata is optional */ }
        } else if (name === 'error') {
          try { providerError(JSON.parse(data)); } catch { fail('AI_PROVIDER_UNAVAILABLE'); }
        } else if (data === '[DONE]') {
          settled = true;
          callbacks.onEnd();
        } else {
          try {
            const chunk = JSON.parse(data) as ChatCompletionChunk;
            const content = chunk.choices?.[0]?.delta?.content;
            if (content) callbacks.onChunk(content);
          } catch { fail('AI_STREAM_INCOMPLETE'); }
        }
      };
      const consumeLine = (line: string) => {
        const trimmed = line.replace(/\r$/, '');
        if (!trimmed) { flushEvent(); return; }
        if (trimmed.startsWith(':')) return;
        if (trimmed.startsWith('event:')) eventName = trimmed.slice(6).trim();
        if (trimmed.startsWith('data:')) dataLines.push(trimmed.slice(5).trimStart());
      };
      while (!settled) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';
        for (const line of lines) {
          consumeLine(line);
          if (settled) break;
        }
      }
      buffer += decoder.decode();
      if (buffer) consumeLine(buffer);
      flushEvent();
      if (!settled) fail('AI_STREAM_INCOMPLETE');
    } catch {
      if (!controller.signal.aborted) fail('AI_CONNECTION_FAILED');
    } finally {
      window.clearTimeout(timeout);
      await reader?.cancel().catch(() => undefined);
      reader?.releaseLock();
    }
  };
  void fetchStream();
  return controller;
}
