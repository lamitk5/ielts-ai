export const MAX_HISTORY_MESSAGES = 8

const ERROR_MESSAGES = {
  AI_RATE_LIMITED: 'Trợ giảng AI đang nhận nhiều yêu cầu. Hãy thử lại sau một chút.',
  AI_TEMPORARILY_UNAVAILABLE: 'Trợ giảng AI tạm thời chưa sẵn sàng.',
  AI_TIMEOUT: 'Không thể kết nối tới Trợ giảng AI. Vui lòng thử lại.',
  AI_PROVIDER_ERROR: 'Trợ giảng AI chưa thể trả lời lúc này. Vui lòng thử lại.',
  AI_INVALID_REQUEST: 'Câu hỏi chưa hợp lệ. Vui lòng thử lại với nội dung rõ hơn.',
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
      const code = payload?.error?.code ?? fallbackCode(response.status)
      throw new AiTutorApiError(code, ERROR_MESSAGES[code] ?? ERROR_MESSAGES.AI_PROVIDER_ERROR, response.status)
    }

    if (!payload || typeof payload.answer !== 'string' || !['ANSWERED', 'INSUFFICIENT_CONTEXT'].includes(payload.status)) {
      throw new AiTutorApiError('AI_PROVIDER_ERROR', ERROR_MESSAGES.AI_PROVIDER_ERROR, response.status)
    }

    return {
      status: payload.status,
      answer: payload.answer,
      sources: Array.isArray(payload.sources) ? payload.sources : [],
      grounding: payload.grounding ?? { status: 'NOT_ENABLED', ragEnabled: false },
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

function fallbackCode(status) {
  if (status === 400) return 'AI_INVALID_REQUEST'
  if (status === 429) return 'AI_RATE_LIMITED'
  if (status === 503) return 'AI_TEMPORARILY_UNAVAILABLE'
  if (status === 504) return 'AI_TIMEOUT'
  return 'AI_PROVIDER_ERROR'
}
