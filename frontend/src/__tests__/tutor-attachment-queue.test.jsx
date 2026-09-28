import { act, renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import { useTutorAttachmentQueue } from '../features/tutor/useTutorAttachmentQueue'
import * as api from '../services/tutorAttachmentsApi'

vi.mock('../services/tutorAttachmentsApi', () => ({
  uploadAttachments: vi.fn(),
  getAttachment: vi.fn(),
  deleteAttachment: vi.fn(),
}))

function file(name) {
  return new File([name], name, { type: 'text/plain' })
}

describe('bounded Tutor attachment queue', () => {
  beforeEach(() => vi.clearAllMocks())

  test('limitsConcurrencyToTwo', async () => {
    const pending = []
    api.uploadAttachments.mockImplementation(async ([selected]) => new Promise((resolve) => {
      pending.push({ selected, resolve })
    }))
    const { result } = renderHook(() => useTutorAttachmentQueue({ conversationId: 'c1' }))

    act(() => result.current.addFiles([file('a.txt'), file('b.txt'), file('c.txt')]))
    await waitFor(() => expect(api.uploadAttachments).toHaveBeenCalledTimes(2))
    expect(pending).toHaveLength(2)
  })

  test('keepsFourSuccessesWhenOneFails', async () => {
    api.uploadAttachments.mockImplementation(async ([selected]) => {
      if (selected.name === 'bad.txt') throw new Error('failed')
      return [{ id: selected.name, filename: selected.name, status: 'READY', sizeBytes: selected.size }]
    })
    const { result } = renderHook(() => useTutorAttachmentQueue({ conversationId: 'c1' }))
    act(() => result.current.addFiles(['a.txt', 'b.txt', 'bad.txt', 'd.txt', 'e.txt'].map(file)))

    await waitFor(() => expect(result.current.attachments.filter((item) => item.status === 'READY')).toHaveLength(4))
    expect(result.current.attachments.some((item) => item.status === 'FAILED')).toBe(true)
  })

  test('retriesOnlyFailedFile', async () => {
    api.uploadAttachments
      .mockRejectedValueOnce(new Error('failed'))
      .mockResolvedValueOnce([{ id: 'a2', filename: 'a.txt', status: 'READY', sizeBytes: 1 }])
    const { result } = renderHook(() => useTutorAttachmentQueue({ conversationId: 'c1' }))
    act(() => result.current.addFiles([file('a.txt')]))
    await waitFor(() => expect(result.current.attachments[0].status).toBe('FAILED'))
    act(() => result.current.retry(result.current.attachments[0].localId))
    await waitFor(() => expect(result.current.readyIds).toEqual(['a2']))
    expect(api.uploadAttachments).toHaveBeenCalledTimes(2)
  })

  test('removesOnlyRequestedFile', async () => {
    api.uploadAttachments.mockResolvedValue([{ id: 'a1', filename: 'a.txt', status: 'READY', sizeBytes: 1 }])
    const { result } = renderHook(() => useTutorAttachmentQueue({ conversationId: 'c1' }))
    act(() => result.current.addFiles([file('a.txt'), file('b.txt')]))
    await waitFor(() => expect(result.current.readyIds).toHaveLength(2))
    const first = result.current.attachments[0].localId
    act(() => result.current.remove(first))
    expect(result.current.attachments).toHaveLength(1)
  })

  test('pollsProcessingToReady', async () => {
    vi.useFakeTimers()
    api.uploadAttachments.mockResolvedValue([{ id: 'a1', filename: 'a.txt', status: 'PROCESSING', sizeBytes: 1 }])
    api.getAttachment.mockResolvedValueOnce({ id: 'a1', filename: 'a.txt', status: 'READY', sizeBytes: 1 })
    const { result } = renderHook(() => useTutorAttachmentQueue({ conversationId: 'c1' }))
    act(() => result.current.addFiles([file('a.txt')]))
    await act(async () => { await vi.advanceTimersByTimeAsync(300) })
    expect(result.current.readyIds).toEqual(['a1'])
    vi.useRealTimers()
  })

  test('pollsStoredAttachmentsToReady', async () => {
    api.uploadAttachments.mockResolvedValue([{ id: 'a-stored', filename: 'guide.txt', status: 'STORED', sizeBytes: 1 }])
    api.getAttachment.mockResolvedValueOnce({ id: 'a-stored', filename: 'guide.txt', status: 'READY', sizeBytes: 1 })
    const { result } = renderHook(() => useTutorAttachmentQueue({ conversationId: 'c1' }))

    act(() => result.current.addFiles([file('guide.txt')]))

    await waitFor(() => expect(result.current.readyIds).toEqual(['a-stored']))
  })

  test('releasesPreviewUrls', async () => {
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:a')
    const revoke = vi.spyOn(URL, 'revokeObjectURL')
    api.uploadAttachments.mockResolvedValue([{ id: 'a1', filename: 'a.png', status: 'READY', sizeBytes: 1 }])
    const { result } = renderHook(() => useTutorAttachmentQueue({ conversationId: 'c1' }))
    act(() => result.current.addFiles([new File(['image'], 'a.png', { type: 'image/png' })]))
    await waitFor(() => expect(result.current.readyIds).toEqual(['a1']))
    act(() => result.current.remove(result.current.attachments[0].localId))
    expect(revoke).toHaveBeenCalled()
  })

  test('chatRetryMakesNoUploadCall', async () => {
    api.uploadAttachments.mockResolvedValue([{ id: 'a1', filename: 'a.txt', status: 'READY', sizeBytes: 1 }])
    const { result } = renderHook(() => useTutorAttachmentQueue({ conversationId: 'c1' }))
    act(() => result.current.addFiles([file('a.txt')]))
    await waitFor(() => expect(result.current.readyIds).toEqual(['a1']))
    const uploadCount = api.uploadAttachments.mock.calls.length
    act(() => result.current.markChatRetry())
    expect(api.uploadAttachments).toHaveBeenCalledTimes(uploadCount)
  })
})
