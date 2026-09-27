import { normalizeTutorReferences } from '../features/tutor/tutorReferenceSchema'
import { ASSISTANT_NAME } from '../features/tutor/assistantIdentity'

export const MAX_HISTORY_MESSAGES = 8

const ERROR_MESSAGES = {
  AI_RATE_LIMITED: `${ASSISTANT_NAME} đang nhận nhiều yêu cầu. Hãy thử lại sau một chút.`,
  AI_TEMPORARILY_UNAVAILABLE: `${ASSISTANT_NAME} tạm thời chưa sẵn sàng.`,
  AI_TIMEOUT: `Không thể kết nối tới ${ASSISTANT_NAME}. Vui lòng thử lại.`,
  AI_PROVIDER_ERROR: `${ASSISTANT_NAME} chưa thể trả lời lúc này. Vui lòng thử lại.`,
  AI_INVALID_REQUEST: 'Câu hỏi chưa hợp lệ. Vui lòng thử lại với nội dung rõ hơn.',
  AUTH_REQUIRED: `Vui lòng đăng nhập để sử dụng ${ASSISTANT_NAME}.`,
}

function getAuthHeaders() {
  try {
    const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
    return session?.token ? { Authorization: `Bearer ${session.token}` } : {}
  } catch {
    return {}
  }
}

export class AiTutorApiError extends Error {
  constructor(code, message, status) {
    super(message)
    this.name = 'AiTutorApiError'
    this.code = code
    this.status = status
  }
}

export async function loadLatestTutorConversation(options = {}) {
  const headers = { ...getAuthHeaders() }
  const listResponse = await fetch('/api/ai/conversations', { headers, signal: options.signal })
  if (!listResponse.ok) return null
  const conversations = await listResponse.json().catch(() => [])
  const latest = Array.isArray(conversations)
    ? conversations.find((conversation) => conversation?.status !== 'ARCHIVED')
    : null
  if (!latest?.id) return null

  const detailResponse = await fetch(`/api/ai/conversations/${latest.id}`, { headers, signal: options.signal })
  if (!detailResponse.ok) return null
  const detail = await detailResponse.json().catch(() => null)
  const messages = Array.isArray(detail?.messages)
    ? detail.messages.map((message) => ({
      id: message.id || `history-${Date.now()}-${Math.random()}`,
      role: String(message.role || '').toUpperCase() === 'USER' ? 'user' : 'assistant',
      content: typeof message.content === 'string' ? message.content : '',
      status: message.responseStatus || undefined,
    })).filter((message) => message.content)
    : []
  return { conversationId: latest.id, messages }
}

export async function sendTutorMessage({ message, context = { skill: 'GENERAL' }, history = [], conversationId = null }, options = {}) {
  const controller = new AbortController()
  const timeout = options.timeoutMs ?? 20000
  const timeoutId = window.setTimeout(() => controller.abort(), timeout)
  const removeAbortListener = options.signal
    ? (() => {
        const abort = () => controller.abort()
        options.signal.addEventListener('abort', abort, { once: true })
        return () => options.signal.removeEventListener('abort', abort)
      })()
    : () => {}

  try {
    const response = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify({
        message,
        context,
        ...(conversationId
          ? { conversationId }
          : { history: history.slice(-MAX_HISTORY_MESSAGES) }),
      }),
      signal: controller.signal,
    })
    const payload = await response.json().catch(() => null)

    if (!response.ok) {
      const code = response.status === 401 ? 'AUTH_REQUIRED' : (payload?.error?.code ?? fallbackCode(response.status))
      const message = code === 'AUTH_REQUIRED'
        ? ERROR_MESSAGES.AUTH_REQUIRED
        : (ERROR_MESSAGES[code] ?? ERROR_MESSAGES.AI_PROVIDER_ERROR)
      throw new AiTutorApiError(code, message, response.status)
    }

    if (!payload || typeof payload.answer !== 'string'
      || !['ANSWERED', 'APP_DATA', 'INSUFFICIENT_CONTEXT', 'INSUFFICIENT_EVIDENCE', 'FALLBACK', 'OUT_OF_SCOPE', 'INVALID_CONVERSATION'].includes(payload.status)) {
      throw new AiTutorApiError('AI_PROVIDER_ERROR', ERROR_MESSAGES.AI_PROVIDER_ERROR, response.status)
    }

    return {
      status: payload.status,
      answer: payload.answer,
      sources: normalizeSources(payload.sources),
      grounding: normalizeGrounding(payload.grounding),
      references: normalizeTutorReferences(payload.references),
      conversationId: typeof payload.meta?.conversationId === 'string' ? payload.meta.conversationId : null,
      meta: payload.meta,
      timestamp: payload.timestamp,
    }
  } catch (error) {
    if (error instanceof AiTutorApiError) throw error
    if (error?.name === 'AbortError') throw new AiTutorApiError('AI_TIMEOUT', ERROR_MESSAGES.AI_TIMEOUT)
    throw new AiTutorApiError('AI_TEMPORARILY_UNAVAILABLE', `Không thể kết nối tới ${ASSISTANT_NAME}. Vui lòng thử lại.`)
  } finally {
    window.clearTimeout(timeoutId)
    removeAbortListener()
  }
}

function normalizeSources(sources) {
  if (!Array.isArray(sources)) return []

  return sources.map((source) => ({
    sourceId: typeof source?.sourceId === 'string' ? source.sourceId : '',
    title: typeof source?.title === 'string' ? source.title : '',
    section: typeof source?.section === 'string' ? source.section : '',
    version: typeof source?.version === 'string' ? source.version : '',
    page: Number.isFinite(source?.page) ? source.page : null,
    chunkId: typeof source?.chunkId === 'string' ? source.chunkId : '',
  })).filter((source) => source.sourceId && source.title)
}

function normalizeGrounding(grounding) {
  if (!grounding || typeof grounding !== 'object') return { status: 'NOT_ENABLED', ragEnabled: false }

  return {
    status: typeof grounding.status === 'string' ? grounding.status.toUpperCase() : 'NOT_ENABLED',
    ragEnabled: Boolean(grounding.ragEnabled),
  }
}

function fallbackCode(status) {
  if (status === 401) return 'AUTH_REQUIRED'
  if (status === 400) return 'AI_INVALID_REQUEST'
  if (status === 429) return 'AI_RATE_LIMITED'
  if (status === 503) return 'AI_TEMPORARILY_UNAVAILABLE'
  if (status === 504) return 'AI_TIMEOUT'
  return 'AI_PROVIDER_ERROR'
}
