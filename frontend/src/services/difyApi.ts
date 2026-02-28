/**
 * Dify Cloud SSE 流式 API 服务
 * 
 * 使用原生 fetch + ReadableStream 实现 Server-Sent Events 流式传输
 * 不使用 axios，因为 axios 不支持原生 SSE 流式读取
 */

const DIFY_API_URL = import.meta.env.VITE_DIFY_API_URL || 'https://api.dify.ai/v1';
const DIFY_API_KEY = import.meta.env.VITE_DIFY_API_KEY || '';

// ==================== 类型定义 ====================

/** Dify SSE 事件类型 */
export type DifyEventType =
  | 'message'           // 流式输出的文本 chunk
  | 'agent_message'     // Agent 模式的文本 chunk
  | 'agent_thought'     // Agent 推理过程
  | 'message_file'      // 文件消息
  | 'message_end'       // 消息结束
  | 'message_replace'   // 消息替换（内容审核）
  | 'error'             // 错误
  | 'ping';             // 心跳

/** Dify SSE 单个事件数据 */
export interface DifySSEEvent {
  event: DifyEventType;
  task_id?: string;
  message_id?: string;
  conversation_id?: string;
  answer?: string;
  thought?: string;
  metadata?: {
    usage?: {
      total_tokens: number;
      prompt_tokens: number;
      completion_tokens: number;
    };
    retriever_resources?: Array<{
      document_name: string;
      segment_id: string;
      score: number;
      content: string;
    }>;
  };
  code?: number;
  message?: string;
  status?: number;
}

/** 流式回调函数 */
export interface StreamCallbacks {
  /** 每收到一个文本 chunk 时触发 */
  onChunk: (text: string) => void;
  /** 收到 Agent 推理过程时触发 */
  onThought?: (thought: string) => void;
  /** 流结束时触发 */
  onEnd: (conversationId: string, messageId: string) => void;
  /** 发生错误时触发 */
  onError: (error: string) => void;
}

/** 聊天请求参数 */
export interface ChatRequest {
  /** 用户消息 */
  query: string;
  /** 对话 ID（多轮对话时传入） */
  conversationId?: string;
  /** 用户标识 */
  userId: string;
  /** 额外输入变量 */
  inputs?: Record<string, string>;
}

// ==================== 用户 ID 管理 ====================

/**
 * 获取或生成匿名用户 ID
 * 存储在 localStorage 中保持一致性
 */
export function getUserId(): string {
  const STORAGE_KEY = 'dify-user-id';
  let userId = localStorage.getItem(STORAGE_KEY);
  if (!userId) {
    userId = `blog-visitor-${crypto.randomUUID()}`;
    localStorage.setItem(STORAGE_KEY, userId);
  }
  return userId;
}

// ==================== SSE 解析器 ====================

/**
 * 解析 SSE 文本流中的事件
 * SSE 格式: "data: {json}\n\n"
 */
function parseSSEEvents(text: string): DifySSEEvent[] {
  const events: DifySSEEvent[] = [];
  // 按 \n\n 分割事件
  const chunks = text.split('\n\n').filter(Boolean);

  for (const chunk of chunks) {
    const lines = chunk.split('\n');
    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const jsonStr = line.slice(6).trim();
        if (jsonStr) {
          try {
            const data = JSON.parse(jsonStr) as DifySSEEvent;
            events.push(data);
          } catch {
            // 解析失败跳过（可能是不完整的 JSON）
            console.warn('[DifyAPI] JSON parse failed:', jsonStr);
          }
        }
      }
    }
  }

  return events;
}

// ==================== 核心 API ====================

/**
 * 发送聊天消息（SSE 流式）
 * 
 * @param request - 聊天请求参数
 * @param callbacks - 流式回调函数
 * @returns AbortController 用于取消请求
 */
export function sendChatMessage(
  request: ChatRequest, 
  callbacks: StreamCallbacks
): AbortController {
  const controller = new AbortController();

  // 检查 API Key 配置
  if (!DIFY_API_KEY || DIFY_API_KEY === 'app-你的Dify应用API密钥') {
    callbacks.onError('AI 助手尚未配置，请先设置 Dify API Key');
    return controller;
  }

  const fetchStream = async () => {
    try {
      const response = await fetch(`${DIFY_API_URL}/chat-messages`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${DIFY_API_KEY}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          inputs: request.inputs || {},
          query: request.query,
          response_mode: 'streaming',
          conversation_id: request.conversationId || '',
          user: request.userId,
        }),
        signal: controller.signal,
      });

      // 检查 HTTP 状态
      if (!response.ok) {
        const errorText = await response.text();
        let errorMsg = '请求失败';

        switch (response.status) {
          case 400:
            errorMsg = '请求参数错误，请稍后重试';
            break;
          case 401:
            errorMsg = 'API 密钥无效，请检查配置';
            break;
          case 404:
            errorMsg = 'AI 应用不存在，请检查配置';
            break;
          case 429:
            errorMsg = '请求太频繁，请稍后再试';
            break;
          case 500:
            errorMsg = 'AI 服务暂时不可用，请稍后重试';
            break;
          default:
            try {
              const errorJson = JSON.parse(errorText);
              errorMsg = errorJson.message || errorMsg;
            } catch {
              errorMsg = `请求失败 (${response.status})`;
            }
        }

        callbacks.onError(errorMsg);
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

        // 尝试按 \n\n 分割完整的事件
        const parts = buffer.split('\n\n');
        // 最后一部分可能不完整，保留在 buffer 中
        buffer = parts.pop() || '';

        const fullChunks = parts.filter(Boolean).join('\n\n');
        if (!fullChunks) continue;

        const events = parseSSEEvents(fullChunks + '\n\n');

        for (const event of events) {
          switch (event.event) {
            case 'message':
            case 'agent_message':
              if (event.answer) {
                callbacks.onChunk(event.answer);
              }
              break;

            case 'agent_thought':
              if (event.thought && callbacks.onThought) {
                callbacks.onThought(event.thought);
              }
              break;

            case 'message_end':
              callbacks.onEnd(
                event.conversation_id || '',
                event.message_id || ''
              );
              return;

            case 'message_replace':
              // 内容审核替换，暂不处理
              break;

            case 'error':
              callbacks.onError(event.message || '未知错误');
              return;

            case 'ping':
              // 心跳，忽略
              break;
          }
        }
      }

      // 处理 buffer 中残余的数据
      if (buffer.trim()) {
        const events = parseSSEEvents(buffer + '\n\n');
        for (const event of events) {
          if ((event.event === 'message' || event.event === 'agent_message') && event.answer) {
            callbacks.onChunk(event.answer);
          }
          if (event.event === 'message_end') {
            callbacks.onEnd(
              event.conversation_id || '',
              event.message_id || ''
            );
            return;
          }
        }
      }

    } catch (err: unknown) {
      if (err instanceof DOMException && err.name === 'AbortError') {
        // 用户主动取消，不报错
        return;
      }
      const errorMsg = err instanceof Error ? err.message : '网络连接失败';
      callbacks.onError(`连接失败: ${errorMsg}`);
    }
  };

  fetchStream();
  return controller;
}

/**
 * 停止正在进行的流式对话
 * 调用 Dify 的 stop 接口
 */
export async function stopChatMessage(taskId: string, userId: string): Promise<void> {
  try {
    await fetch(`${DIFY_API_URL}/chat-messages/${taskId}/stop`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${DIFY_API_KEY}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ user: userId }),
    });
  } catch {
    // stop 请求失败不影响前端
    console.warn('[DifyAPI] Stop request failed');
  }
}
