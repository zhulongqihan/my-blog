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

export interface StreamCallbacks {
  onChunk: (text: string) => void;
  onEnd: () => void;
  onError: (error: string) => void;
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

  const fetchStream = async () => {
    try {
      const response = await fetch(`${AI_API_URL}/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
        signal: controller.signal,
      });

      if (!response.ok) {
        const text = await response.text().catch(() => '');
        const messages: Record<number, string> = {
          400: '请求参数错误，请稍后重试',
          401: '认证信息无效，请稍后重试',
          404: 'AI Gateway 地址不存在',
          429: '请求太频繁，请稍后再试',
          500: 'AI 服务暂时不可用，请稍后重试',
          502: 'AI 服务网关暂时不可用，请稍后重试',
        };
        let message = messages[response.status] || `请求失败 (${response.status})`;
        try { message = JSON.parse(text).message || message; } catch { /* use status fallback */ }
        callbacks.onError(message);
        return;
      }

      const reader = response.body!.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';
      let eventName = '';
      let dataLines: string[] = [];
      let ended = false;

      const flushEvent = () => {
        if (dataLines.length === 0) return;
        const data = dataLines.join('\n');
        dataLines = [];
        if (eventName === 'meta') {
          try { callbacks.onMeta?.(JSON.parse(data) as AiStreamMeta); } catch { /* ignore malformed metadata */ }
        } else if (eventName === 'error') {
          ended = true;
          try { callbacks.onError(JSON.parse(data).message || 'AI 服务请求失败'); } catch { callbacks.onError('AI 服务请求失败'); }
        } else if (data === '[DONE]') {
          ended = true;
          callbacks.onEnd();
        } else {
          try {
            const chunk = JSON.parse(data) as ChatCompletionChunk;
            const content = chunk.choices?.[0]?.delta?.content;
            if (content) callbacks.onChunk(content);
            if (chunk.choices?.[0]?.finish_reason === 'stop' && !ended) {
              ended = true;
              callbacks.onEnd();
            }
          } catch { /* ignore malformed provider chunk */ }
        }
        eventName = '';
      };

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';
        for (const line of lines) {
          const trimmed = line.trim();
          if (!trimmed) { flushEvent(); continue; }
          if (trimmed.startsWith(':')) continue;
          if (trimmed.startsWith('event:')) eventName = trimmed.slice(6).trim();
          if (trimmed.startsWith('data:')) dataLines.push(trimmed.slice(5).trimStart());
        }
      }
      flushEvent();
      if (!ended) callbacks.onEnd();
    } catch (error: unknown) {
      if (error instanceof DOMException && error.name === 'AbortError') return;
      callbacks.onError(`连接失败: ${error instanceof Error ? error.message : '网络连接失败'}`);
    }
  };

  fetchStream();
  return controller;
}
