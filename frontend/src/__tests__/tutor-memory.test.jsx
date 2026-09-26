import { describe, expect, test, vi } from 'vitest'
import { sendTutorMessage } from '../services/aiTutorApi'

describe('Tutor conversation memory contract', () => {
  test('sends additive conversation reference and accepts insufficient evidence state', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        status: 'INSUFFICIENT_EVIDENCE',
        answer: 'Chưa đủ bằng chứng.',
        sources: [],
        grounding: { status: 'INSUFFICIENT_EVIDENCE', ragEnabled: true },
        meta: { requestId: 'r-1', conversationId: '11111111-1111-4111-8111-111111111111' },
      }),
    }))

    const result = await sendTutorMessage({
      message: 'Explain this source',
      context: { skill: 'READING' },
      history: [],
      conversationId: '11111111-1111-4111-8111-111111111111',
    })

    const payload = JSON.parse(fetch.mock.calls[0][1].body)
    expect(payload.conversationId).toBe('11111111-1111-4111-8111-111111111111')
    expect(result.status).toBe('INSUFFICIENT_EVIDENCE')
    expect(result.conversationId).toBe('11111111-1111-4111-8111-111111111111')
  })
})
