import { beforeEach, describe, expect, test, vi } from 'vitest'
import { loadLatestTutorConversation, sendTutorMessage } from '../services/aiTutorApi'

describe('multimodal Tutor regression boundaries', () => {
  beforeEach(() => vi.stubGlobal('fetch', vi.fn()))

  test('textOnlyProviderCannotReceiveVisionTurn', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({
      status: 'ANSWERED', answer: 'hello', sources: [], grounding: { status: 'NOT_ENABLED', ragEnabled: false },
    }) })
    await sendTutorMessage({ message: 'hello' })
    expect(JSON.parse(fetch.mock.calls[0][1].body)).not.toHaveProperty('attachmentIds')
  })

  test('retryUsesExistingIds', async () => {
    fetch.mockResolvedValue({ ok: true, status: 200, json: async () => ({
      status: 'ANSWERED', answer: 'ok', sources: [], grounding: { status: 'NOT_ENABLED', ragEnabled: false },
    }) })
    await sendTutorMessage({ message: 'retry', conversationId: 'c1', attachmentIds: ['a1'] })
    await sendTutorMessage({ message: 'retry', conversationId: 'c1', attachmentIds: ['a1'] })
    expect(fetch).toHaveBeenCalledTimes(2)
    expect(JSON.parse(fetch.mock.calls[1][1].body).attachmentIds).toEqual(['a1'])
  })

  test('reloadPreservesMetadata', async () => {
    fetch
      .mockResolvedValueOnce({ ok: true, json: async () => [{ id: 'c1', status: 'ACTIVE' }] })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ messages: [{ id: 'm1', role: 'USER', content: 'see', attachments: [{ id: 'a1', filename: 'photo.png', attachmentKind: 'IMAGE', sizeBytes: 12 }] }] }) })
    const result = await loadLatestTutorConversation()
    expect(result.messages[0].attachments[0]).toMatchObject({ id: 'a1', filename: 'photo.png', kind: 'IMAGE' })
  })
})
