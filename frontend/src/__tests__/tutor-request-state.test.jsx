import { describe, expect, test, vi } from 'vitest'
import {
  TUTOR_REQUEST_STATES,
  GENERIC_PENDING_LABEL,
  INITIAL_TUTOR_REQUEST_STATE,
  tutorRequestReducer,
} from '../features/tutor/tutorRequestState'

describe('Task 7: Truthful Tutor Request States', () => {
  test('initial state is IDLE', () => {
    expect(INITIAL_TUTOR_REQUEST_STATE.status).toBe(TUTOR_REQUEST_STATES.IDLE)
    expect(INITIAL_TUTOR_REQUEST_STATE.pendingLabel).toBeNull()
  })

  test('transitions to PENDING_GENERIC on START_REQUEST with honest generic copy', () => {
    const next = tutorRequestReducer(INITIAL_TUTOR_REQUEST_STATE, {
      type: 'START_REQUEST',
      requestId: 'req-123',
    })

    expect(next.status).toBe(TUTOR_REQUEST_STATES.PENDING_GENERIC)
    expect(next.pendingLabel).toBe(GENERIC_PENDING_LABEL)
    expect(next.pendingLabel).not.toMatch(/ngữ pháp|từ vựng|chấm điểm/i) // No fake stage progression
  })

  test('transitions to ANSWERED on RECEIVE_RESPONSE for standard answers', () => {
    const pendingState = {
      status: TUTOR_REQUEST_STATES.PENDING_GENERIC,
      error: null,
      requestId: 'req-123',
      pendingLabel: GENERIC_PENDING_LABEL,
    }

    const next = tutorRequestReducer(pendingState, {
      type: 'RECEIVE_RESPONSE',
      status: 'ANSWERED',
    })

    expect(next.status).toBe(TUTOR_REQUEST_STATES.ANSWERED)
    expect(next.pendingLabel).toBeNull()
    expect(next.error).toBeNull()
  })

  test('transitions to INSUFFICIENT_CONTEXT on RECEIVE_RESPONSE with INSUFFICIENT_CONTEXT', () => {
    const pendingState = {
      status: TUTOR_REQUEST_STATES.PENDING_GENERIC,
      error: null,
      requestId: 'req-123',
      pendingLabel: GENERIC_PENDING_LABEL,
    }

    const next = tutorRequestReducer(pendingState, {
      type: 'RECEIVE_RESPONSE',
      status: 'INSUFFICIENT_CONTEXT',
    })

    expect(next.status).toBe(TUTOR_REQUEST_STATES.INSUFFICIENT_CONTEXT)
  })

  test('transitions to CANCELLED on user cancel action', () => {
    const pendingState = {
      status: TUTOR_REQUEST_STATES.PENDING_GENERIC,
      error: null,
      requestId: 'req-123',
      pendingLabel: GENERIC_PENDING_LABEL,
    }

    const next = tutorRequestReducer(pendingState, { type: 'CANCEL_REQUEST' })
    expect(next.status).toBe(TUTOR_REQUEST_STATES.CANCELLED)
    expect(next.pendingLabel).toBeNull()
  })

  test('transitions to TIMEOUT on request abort/timeout failure', () => {
    const pendingState = {
      status: TUTOR_REQUEST_STATES.PENDING_GENERIC,
      error: null,
      requestId: 'req-123',
      pendingLabel: GENERIC_PENDING_LABEL,
    }

    const next = tutorRequestReducer(pendingState, {
      type: 'FAIL_REQUEST',
      error: { code: 'AI_TIMEOUT', message: 'Hết thời gian chờ phản hồi.' },
    })

    expect(next.status).toBe(TUTOR_REQUEST_STATES.TIMEOUT)
    expect(next.error).toBe('Hết thời gian chờ phản hồi.')
  })

  test('transitions to RATE_LIMITED on 429 error code', () => {
    const pendingState = {
      status: TUTOR_REQUEST_STATES.PENDING_GENERIC,
      error: null,
      requestId: 'req-123',
      pendingLabel: GENERIC_PENDING_LABEL,
    }

    const next = tutorRequestReducer(pendingState, {
      type: 'FAIL_REQUEST',
      error: { code: 'AI_RATE_LIMITED', message: 'Đang nhận nhiều yêu cầu.' },
    })

    expect(next.status).toBe(TUTOR_REQUEST_STATES.RATE_LIMITED)
  })

  test('transitions to UNAVAILABLE on 503 error code', () => {
    const pendingState = {
      status: TUTOR_REQUEST_STATES.PENDING_GENERIC,
      error: null,
      requestId: 'req-123',
      pendingLabel: GENERIC_PENDING_LABEL,
    }

    const next = tutorRequestReducer(pendingState, {
      type: 'FAIL_REQUEST',
      error: { code: 'AI_TEMPORARILY_UNAVAILABLE', message: 'Hệ thống tạm thời bảo trì.' },
    })

    expect(next.status).toBe(TUTOR_REQUEST_STATES.UNAVAILABLE)
  })

  test('extension state constants exist for future event streaming without being executed', () => {
    expect(TUTOR_REQUEST_STATES.PREPARING).toBe('PREPARING')
    expect(TUTOR_REQUEST_STATES.PROCESSING).toBe('PROCESSING')
    expect(TUTOR_REQUEST_STATES.FINALIZING).toBe('FINALIZING')
  })
})
