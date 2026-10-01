import { beforeEach, describe, expect, test, vi } from 'vitest'
import { loadLatestTutorConversation, sendTutorMessage } from '../services/aiTutorApi'
import { normalizeAttachmentSource } from '../features/tutor/attachmentSourceSchema'
import { uploadAttachments } from '../services/tutorAttachmentsApi'

describe('Én multimodal API contracts', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
    localStorage.clear()
  })

  test('uploadSendsRepeatedFilesAndConversationId', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 201, json: async () => ({ attachments: [{ id: 'a1' }] }) })
    const files = [
      new File(['one'], 'one.txt', { type: 'text/plain' }),
      new File(['two'], 'two.pdf', { type: 'application/pdf' }),
    ]

    const result = await uploadAttachments(files, 'conversation-1')

    const body = fetch.mock.calls[0][1].body
    expect(body.getAll('files')).toHaveLength(2)
    expect(body.get('conversationId')).toBe('conversation-1')
    expect(result).toHaveLength(1)
  })

  test('normalizesBatchAndLegacyOneFileResponses', async () => {
    fetch
      .mockResolvedValueOnce({ ok: true, status: 201, json: async () => ({ attachments: [{ id: 'a1', filename: 'one.txt' }] }) })
      .mockResolvedValueOnce({ ok: true, status: 201, json: async () => ({ id: 'a2', filename: 'two.txt' }) })

    expect((await uploadAttachments([new File(['one'], 'one.txt')], 'c'))[0].id).toBe('a1')
    expect((await uploadAttachments([new File(['two'], 'two.txt')], 'c'))[0].id).toBe('a2')
  })

  test('sendsOnlyAttachmentIdsInChatJson', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({
      status: 'ANSWERED', answer: 'ok', sources: [], grounding: { status: 'NOT_ENABLED', ragEnabled: false },
    }) })

    await sendTutorMessage({ message: 'đọc tệp', conversationId: 'c1', attachmentIds: ['a1', 'a2'] })

    const body = JSON.parse(fetch.mock.calls[0][1].body)
    expect(body.attachmentIds).toEqual(['a1', 'a2'])
    expect(body).not.toHaveProperty('files')
  })

  test('rejectsDuplicateIdsClientSide', async () => {
    await expect(sendTutorMessage({ message: 'duplicate', attachmentIds: ['a1', 'a1'] }))
      .rejects.toMatchObject({ code: 'ATTACHMENT_IDS_DUPLICATED' })
    expect(fetch).not.toHaveBeenCalled()
  })

  test('normalizesAttachmentSources', () => {
    expect(normalizeAttachmentSource({ id: 'a1', filename: 'essay.pdf', attachmentKind: 'DOCUMENT', sizeBytes: 20 }))
      .toMatchObject({ id: 'a1', filename: 'essay.pdf', kind: 'DOCUMENT', sizeBytes: 20 })
  })

  test('preservesExistingProviderErrors', async () => {
    fetch.mockResolvedValueOnce({ ok: false, status: 429, json: async () => ({ error: { code: 'AI_RATE_LIMITED' } }) })
    await expect(sendTutorMessage({ message: 'hello' })).rejects.toMatchObject({ code: 'AI_RATE_LIMITED', status: 429 })
  })

  test('historyCarriesSafeAttachmentMetadata', async () => {
    fetch
      .mockResolvedValueOnce({ ok: true, json: async () => [{ id: 'c1', status: 'ACTIVE' }] })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ messages: [{ id: 'm1', role: 'USER', content: 'see', attachments: [{ id: 'a1', filename: 'x.png', attachmentKind: 'IMAGE' }] }] }) })

    const result = await loadLatestTutorConversation()
    expect(result.messages[0].attachments[0]).toMatchObject({ id: 'a1', filename: 'x.png', kind: 'IMAGE' })
  })
})
