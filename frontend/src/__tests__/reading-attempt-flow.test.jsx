import { describe, expect, test, vi } from 'vitest'
import {
  startPracticeAttempt,
  savePracticeAnswers,
  submitReadingAttempt,
} from '../features/reading/practiceApi'

describe('durable Reading attempt contract', () => {
  test('starts a version-bound attempt and supports answer persistence', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => ({ id: 'attempt-1', status: 'IN_PROGRESS' }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ id: 'attempt-1', answers: { 'reading-q1': 'B' } }) })
    vi.stubGlobal('fetch', fetchMock)

    await startPracticeAttempt('reading', 'reading-foundation-01', 'reading-foundation-01:v1', 'reading-key')
    await savePracticeAnswers('attempt-1', { 'reading-q1': 'B' })

    expect(fetchMock.mock.calls[0][0]).toBe('/api/attempts')
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toMatchObject({
      practiceId: 'reading-foundation-01',
      practiceVersion: 'reading-foundation-01:v1',
      skill: 'reading',
      idempotencyKey: 'reading-key',
    })
    expect(fetchMock.mock.calls[1][0]).toBe('/api/attempts/attempt-1/answers')
  })

  test('submits without trusting a browser score and returns persisted result/context IDs', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ id: 'attempt-1', status: 'FEEDBACK_READY', score: 2, total: 2, resultPayload: '{}' }),
    })
    vi.stubGlobal('fetch', fetchMock)

    const result = await submitReadingAttempt('reading', 'reading-foundation-01', 'attempt-1',
      { 'reading-q1': 'B', 'reading-q2': 'A' }, 'reading-key')

    expect(result.status).toBe('FEEDBACK_READY')
    expect(fetchMock.mock.calls[0][0]).toBe('/api/practice/reading/attempts/attempt-1/submit')
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({
      answers: { 'reading-q1': 'B', 'reading-q2': 'A' },
      idempotencyKey: 'reading-key',
    })
  })
})
