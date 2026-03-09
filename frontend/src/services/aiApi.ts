/**
 * AI 创意工坊 — 通义千问 DashScope (OpenAI 兼容接口) SSE 流式服务
 *
 * 使用原生 fetch + ReadableStream 实现 Server-Sent Events 流式传输
 */

const AI_API_URL = import.meta.env.VITE_AI_API_URL || '/ai-api';
const AI_API_KEY = import.meta.env.VITE_AI_API_KEY || '';
const AI_MODEL = import.meta.env.VITE_AI_MODEL || 'qwen-plus';

// ==================== 类型定义 ====================

/** OpenAI 兼容 SSE delta */
interface ChatCompletionChunk {
  id: string;
  object: string;
  created: number;
  model: string;
  choices: Array<{
    index: number;
    delta: { role?: string; content?: string };
    finish_reason: string | null;
  }>;
}

/** 流式回调 */
export interface StreamCallbacks {
  /** 每收到一段文本 */
  onChunk: (text: string) => void;
  /** 流结束 */
  onEnd: () => void;
  /** 错误 */
  onError: (error: string) => void;
}

/** 聊天请求 */
export interface ChatRequest {
  /** 系统提示词 */
  systemPrompt?: string;
  /** 用户消息 */
  query: string;
}

// ==================== 用户 ID 管理 ====================

export function getUserId(): string {
  const KEY = 'ai-user-id';
  let id = localStorage.getItem(KEY);
  if (!id) {
    const uuid =
      globalThis.crypto?.randomUUID?.() ||
      `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
    id = `blog-visitor-${uuid}`;
    localStorage.setItem(KEY, id);
  }
  return id;
}

// ==================== 核心 API ====================

/**
 * 发送聊天消息 (SSE 流式)
 * 使用 OpenAI 兼容的 /chat/completions 接口
 */
export function sendChatMessage(
  request: ChatRequest,
  callbacks: StreamCallbacks,
): AbortController {
  const controller = new AbortController();
  const isProxy = AI_API_URL.startsWith('/');

  // 开发环境检查 Key
  if (!isProxy && !AI_API_KEY) {
    callbacks.onError('AI 创意工坊尚未配置 API Key');
    return controller;
  }

  const fetchStream = async () => {
    try {
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };

      // 非代理模式（开发环境直连）需要附加 Key
      if (AI_API_KEY) {
        headers.Authorization = `Bearer ${AI_API_KEY}`;
      }

      const messages: Array<{ role: string; content: string }> = [];
      if (request.systemPrompt) {
        messages.push({ role: 'system', content: request.systemPrompt });
      }
      messages.push({ role: 'user', content: request.query });

      const response = await fetch(`${AI_API_URL}/chat/completions`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          model: AI_MODEL,
          messages,
          stream: true,
          temperature: 0.7,
          max_tokens: 2048,
        }),
        signal: controller.signal,
      });

      if (!response.ok) {
        const text = await response.text().catch(() => '');
        let msg = '请求失败';
        switch (response.status) {
          case 400: msg = '请求参数错误，请稍后重试'; break;
          case 401: msg = 'API 密钥无效，请检查配置'; break;
          case 502: msg = 'AI 服务网关暂时不可用，请稍后重试'; break;
          case 429: msg = '请求太频繁，请稍后再试'; break;
          case 500: msg = 'AI 服务暂时不可用，请稍后重试'; break;
          default:
            try { msg = JSON.parse(text).error?.message || msg; } catch { msg = `请求失败 (${response.status})`; }
        }
        callbacks.onError(msg);
        return;
      }

      // 读取 SSE 流
      const reader = response.body!.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        // 按行解析 SSE
        const lines = buffer.split('\n');
        buffer = lines.pop() || ''; // 最后一行可能不完整

        for (const line of lines) {
          const trimmed = line.trim();
          if (!trimmed || trimmed.startsWith(':')) continue; // 空行或注释
          if (!trimmed.startsWith('data: ')) continue;

          const data = trimmed.slice(6);
          if (data === '[DONE]') {
            callbacks.onEnd();
            return;
          }

          try {
            const chunk: ChatCompletionChunk = JSON.parse(data);
            const content = chunk.choices?.[0]?.delta?.content;
            if (content) {
              callbacks.onChunk(content);
            }
            if (chunk.choices?.[0]?.finish_reason === 'stop') {
              callbacks.onEnd();
              return;
            }
          } catch {
            // 不完整 JSON，跳过
          }
        }
      }

      // 流正常结束但没收到 [DONE]
      callbacks.onEnd();
    } catch (err: unknown) {
      if (err instanceof DOMException && err.name === 'AbortError') return;
      const msg = err instanceof Error ? err.message : '网络连接失败';
      callbacks.onError(`连接失败: ${msg}`);
    }
  };

  fetchStream();
  return controller;
}
