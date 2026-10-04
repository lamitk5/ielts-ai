import { describe, expect, test, vi } from 'vitest'
import { sendTutorMessage } from '../services/aiTutorApi'

describe('Phase 2B provider-neutral regression', () => {
  test('renders normalized fallback data without exposing provider payload fields', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        status: 'FALLBACK',
        answer: 'Mình vẫn có thể tiếp tục từ ngữ cảnh hiện tại.',
        sources: [],
        grounding: { status: 'NOT_ENABLED', ragEnabled: false },
        provider: 'groq',
        rawError: 'provider detail must not cross the boundary',
      }),
    }))

    const result = await sendTutorMessage({ message: 'hello' })

    expect(result).toEqual(expect.objectContaining({ status: 'FALLBACK' }))
    expect(result).not.toHaveProperty('provider')
    expect(result).not.toHaveProperty('rawError')
    expect(JSON.stringify(result)).not.toMatch(/groq|provider detail/i)
  })
})
