import { describe, expect, test, vi } from 'vitest'
import { startSpeakingAttempt, saveSpeakingAttemptDraft, submitSpeakingAttempt } from '../features/speaking/speakingApi'

describe('durable speaking attempt flow', () => {
  test('starts, saves and submits one attempt through provider-neutral endpoints', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => ({ attemptId: 'attempt-1', status: 'IN_PROGRESS' }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ attemptId: 'attempt-1', status: 'INPUT_SAVED', transcript: 'I enjoy reading.' }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ attemptId: 'attempt-1', status: 'INPUT_SAVED', transcript: 'I enjoy reading.', overallBandEstimate: null }) })
    vi.stubGlobal('fetch', fetchMock)

    const started = await startSpeakingAttempt('speaking-p1-01')
    const draft = await saveSpeakingAttemptDraft('attempt-1', 'I enjoy reading.')
    const submitted = await submitSpeakingAttempt('attempt-1', 'I enjoy reading.')

    expect(started.attemptId).toBe('attempt-1')
    expect(draft.status).toBe('INPUT_SAVED')
    expect(submitted.overallBandEstimate).toBeNull()
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(fetchMock.mock.calls[0][0]).toBe('/api/practice/speaking/attempts/start')
    expect(fetchMock.mock.calls[1][0]).toBe('/api/practice/speaking/attempts/attempt-1/draft')
    expect(fetchMock.mock.calls[2][0]).toBe('/api/practice/speaking/attempts/attempt-1/submit')
  })
})
