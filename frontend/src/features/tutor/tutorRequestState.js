export const TUTOR_REQUEST_STATES = Object.freeze({
  IDLE: 'IDLE',
  PENDING_GENERIC: 'PENDING_GENERIC',
  ANSWERED: 'ANSWERED',
  INSUFFICIENT_CONTEXT: 'INSUFFICIENT_CONTEXT',
  TIMEOUT: 'TIMEOUT',
  RATE_LIMITED: 'RATE_LIMITED',
  CANCELLED: 'CANCELLED',
  UNAVAILABLE: 'UNAVAILABLE',
  ERROR: 'ERROR',
  // Future event-driven extension states (reserved, not used by HTTP transport)
  PREPARING: 'PREPARING',
  PROCESSING: 'PROCESSING',
  FINALIZING: 'FINALIZING',
})

export const GENERIC_PENDING_LABEL = 'Đang chuẩn bị phản hồi…'

export const INITIAL_TUTOR_REQUEST_STATE = Object.freeze({
  status: TUTOR_REQUEST_STATES.IDLE,
  error: null,
  requestId: null,
  pendingLabel: null,
})

export function tutorRequestReducer(state, action) {
  if (!action || typeof action !== 'object') return state

  switch (action.type) {
    case 'START_REQUEST':
      return {
        status: TUTOR_REQUEST_STATES.PENDING_GENERIC,
        error: null,
        requestId: action.requestId ?? null,
        pendingLabel: GENERIC_PENDING_LABEL,
      }

    case 'RECEIVE_RESPONSE':
      if (action.status === 'INSUFFICIENT_CONTEXT') {
        return {
          status: TUTOR_REQUEST_STATES.INSUFFICIENT_CONTEXT,
          error: null,
          requestId: state?.requestId ?? null,
          pendingLabel: null,
        }
      }
      return {
        status: TUTOR_REQUEST_STATES.ANSWERED,
        error: null,
        requestId: state?.requestId ?? null,
        pendingLabel: null,
      }

    case 'CANCEL_REQUEST':
      return {
        status: TUTOR_REQUEST_STATES.CANCELLED,
        error: null,
        requestId: null,
        pendingLabel: null,
      }

    case 'FAIL_REQUEST': {
      const code = action.error?.code || action.code
      let mappedStatus = TUTOR_REQUEST_STATES.ERROR
      if (code === 'AI_TIMEOUT' || action.error?.name === 'AbortError') {
        mappedStatus = TUTOR_REQUEST_STATES.TIMEOUT
      } else if (code === 'AI_RATE_LIMITED' || action.status === 429) {
        mappedStatus = TUTOR_REQUEST_STATES.RATE_LIMITED
      } else if (code === 'AI_TEMPORARILY_UNAVAILABLE' || action.status === 503) {
        mappedStatus = TUTOR_REQUEST_STATES.UNAVAILABLE
      }

      return {
        status: mappedStatus,
        error: action.error?.message || action.message || 'Đã có lỗi xảy ra.',
        requestId: null,
        pendingLabel: null,
      }
    }

    case 'RESET':
      return { ...INITIAL_TUTOR_REQUEST_STATE }

    default:
      return state
  }
}
