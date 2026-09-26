import { normalizeTutorReferences } from '../features/tutor/tutorReferenceSchema'

export const MAX_HISTORY_MESSAGES = 8

const ERROR_MESSAGES = {
  AI_RATE_LIMITED: 'Trợ giảng AI đang nhận nhiều yêu cầu. Hãy thử lại sau một chút.',
  AI_TEMPORARILY_UNAVAILABLE: 'Trợ giảng AI tạm thời chưa sẵn sàng.',
  AI_TIMEOUT: 'Không thể kết nối tới Trợ giảng AI. Vui lòng thử lại.',
  AI_PROVIDER_ERROR: 'Trợ giảng AI chưa thể trả lời lúc này. Vui lòng thử lại.',
  AI_INVALID_REQUEST: 'Câu hỏi chưa hợp lệ. Vui lòng thử lại với nội dung rõ hơn.',
  AUTH_REQUIRED: 'Vui lòng đăng nhập để sử dụng Trợ giảng AI.',
}

export class AiTutorApiError extends Error {
  constructor(code, message, status) {
    super(message)
    this.name = 'AiTutorApiError'
    this.code = code
    this.status = status
  }
}

export async function sendTutorMessage({ message, context = { skill: 'GENERAL' }, history = [] }, options = {}) {
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
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message, context, history: history.slice(-MAX_HISTORY_MESSAGES) }),
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
      || !['ANSWERED', 'APP_DATA', 'INSUFFICIENT_CONTEXT', 'FALLBACK'].includes(payload.status)) {
      throw new AiTutorApiError('AI_PROVIDER_ERROR', ERROR_MESSAGES.AI_PROVIDER_ERROR, response.status)
    }

    return {
      status: payload.status,
      answer: payload.answer,
      sources: normalizeSources(payload.sources),
      grounding: normalizeGrounding(payload.grounding),
      references: normalizeTutorReferences(payload.references),
      meta: payload.meta,
      timestamp: payload.timestamp,
    }
  } catch (error) {
    if (error instanceof AiTutorApiError) throw error
    if (error?.name === 'AbortError') throw new AiTutorApiError('AI_TIMEOUT', ERROR_MESSAGES.AI_TIMEOUT)
    throw new AiTutorApiError('AI_TEMPORARILY_UNAVAILABLE', 'Không thể kết nối tới Trợ giảng AI. Vui lòng thử lại.')
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
