import { describe, expect, test, vi } from 'vitest'
import { startWritingAttempt, saveWritingAttemptDraft, submitWritingAttempt } from '../features/writing/writingApi'

describe('durable Writing attempt contract', () => {
  test('starts, saves draft and submits through the provider-neutral attempt API', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => ({ id: 'writing-attempt-1', status: 'IN_PROGRESS' }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ id: 'writing-attempt-1', status: 'IN_PROGRESS' }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ id: 'writing-attempt-1', status: 'FEEDBACK_READY', assessment: { status: 'UNAVAILABLE' } }) })
    vi.stubGlobal('fetch', fetchMock)

    await startWritingAttempt('task-1-academic-01')
    await saveWritingAttemptDraft('writing-attempt-1', 'A durable writing draft.')
    const result = await submitWritingAttempt('writing-attempt-1', 'A sufficiently long writing response.')

    expect(fetchMock.mock.calls[0][0]).toBe('/api/practice/writing/attempts')
    expect(fetchMock.mock.calls[1][0]).toBe('/api/practice/writing/attempts/writing-attempt-1/draft')
    expect(fetchMock.mock.calls[2][0]).toBe('/api/practice/writing/attempts/writing-attempt-1/submit')
    expect(result.assessment.status).toBe('UNAVAILABLE')
  })
})
