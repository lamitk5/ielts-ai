import { act, renderHook, waitFor } from '@testing-library/react'
import { StrictMode } from 'react'
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

function realisticDocx(name = 'Câu 1.docx', type = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document') {
  return new File([new Uint8Array(1024 * 1024)], name, { type })
}

describe('bounded Tutor attachment queue', () => {
  beforeEach(() => vi.clearAllMocks())

  test('rendersSelectedFileBeforeConversationCreationAndStartsWhenConversationIsReady', async () => {
    api.uploadAttachments.mockResolvedValue([{ id: 'a-pending', filename: 'a.txt', status: 'READY', sizeBytes: 1 }])
    const { result, rerender } = renderHook(
      ({ conversationId }) => useTutorAttachmentQueue({ conversationId }),
      { initialProps: { conversationId: null } },
    )

    act(() => result.current.addFiles([file('a.txt')]))

    expect(result.current.attachments[0]).toMatchObject({ filename: 'a.txt', status: 'SELECTED' })
    expect(api.uploadAttachments).not.toHaveBeenCalled()

    rerender({ conversationId: 'c1' })
    await waitFor(() => expect(result.current.readyIds).toEqual(['a-pending']))
  })

  test('resumes every pending file when conversation creation returns an id', async () => {
    api.uploadAttachments.mockImplementation(async ([selected]) => ([{
      id: `${selected.name}-ready`,
      filename: selected.name,
      status: 'READY',
      sizeBytes: selected.size,
    }]))
    const { result } = renderHook(() => useTutorAttachmentQueue({ conversationId: null }))

    act(() => result.current.addFiles([file('notes.docx'), new File(['image'], 'study.jpg', { type: 'image/jpeg' })]))

    expect(result.current.attachments.map((item) => item.status)).toEqual(['SELECTED', 'SELECTED'])
    act(() => result.current.resumePending('conversation-created'))

    await waitFor(() => expect(api.uploadAttachments).toHaveBeenCalledTimes(2))
    await waitFor(() => expect(result.current.attachments.every((item) => item.status === 'READY')).toBe(true))
    expect(api.uploadAttachments).toHaveBeenNthCalledWith(
      1,
      [expect.objectContaining({ name: 'notes.docx' })],
      'conversation-created',
      expect.any(Object),
    )
  })

  test('uploads a realistic oneMiBDocx after validation prerequisite becomes available', async () => {
    api.uploadAttachments.mockResolvedValue([{ id: 'docx-ready', filename: 'Câu 1.docx', status: 'READY', sizeBytes: 1024 * 1024 }])
    const { result } = renderHook(() => useTutorAttachmentQueue({ conversationId: 'c1' }))

    act(() => result.current.addFiles([realisticDocx()]))

    await waitFor(() => expect(api.uploadAttachments).toHaveBeenCalledWith(
      [expect.objectContaining({ name: 'Câu 1.docx', size: 1024 * 1024 })],
      'c1',
      expect.any(Object),
    ))
    await waitFor(() => expect(result.current.readyIds).toEqual(['docx-ready']))
  })

  test('continues updating attachment state after React StrictMode effect replay', async () => {
    api.uploadAttachments.mockResolvedValue([{ id: 'strict-ready', filename: 'notes.docx', status: 'READY', sizeBytes: 1024 }])
    const wrapper = ({ children }) => <StrictMode>{children}</StrictMode>
    const { result } = renderHook(() => useTutorAttachmentQueue({ conversationId: 'c1' }), { wrapper })

    act(() => result.current.addFiles([realisticDocx('notes.docx')]))

    await waitFor(() => expect(api.uploadAttachments).toHaveBeenCalled())
    await waitFor(() => expect(result.current.attachments[0]).toMatchObject({
      status: 'READY',
      id: 'strict-ready',
    }))
  })

  test('fails a selected file when the validation prerequisite never resolves', async () => {
    vi.useFakeTimers()
    try {
      const { result } = renderHook(() => useTutorAttachmentQueue({ conversationId: null, validationTimeoutMs: 100 }))

      act(() => result.current.addFiles([realisticDocx()]))
      expect(result.current.attachments[0].status).toBe('SELECTED')

      await act(async () => { await vi.advanceTimersByTimeAsync(100) })

      expect(result.current.attachments[0]).toMatchObject({
        status: 'FAILED',
        errorMessage: 'Không thể kiểm tra tệp. Thử lại.',
      })
      expect(api.uploadAttachments).not.toHaveBeenCalled()
    } finally {
      vi.useRealTimers()
    }
  })

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

  test('reselectsTheSameFileAfterRemoval', async () => {
    api.uploadAttachments.mockResolvedValue([{ id: 'same-file', filename: 'same.txt', status: 'READY', sizeBytes: 1 }])
    const selected = file('same.txt')
    const { result } = renderHook(() => useTutorAttachmentQueue({ conversationId: 'c1' }))

    act(() => result.current.addFiles([selected]))
    await waitFor(() => expect(result.current.readyIds).toEqual(['same-file']))
    act(() => result.current.remove(result.current.attachments[0].localId))
    act(() => result.current.addFiles([selected]))

    await waitFor(() => expect(api.uploadAttachments).toHaveBeenCalledTimes(2))
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

  test('stopsPollingWhenAnAttachmentIsRemoved', async () => {
    api.uploadAttachments.mockResolvedValue([{ id: 'a-processing', filename: 'guide.txt', status: 'STORED', sizeBytes: 1 }])
    api.getAttachment.mockResolvedValue({ id: 'a-processing', filename: 'guide.txt', status: 'PROCESSING', sizeBytes: 1 })
    const { result } = renderHook(() => useTutorAttachmentQueue({ conversationId: 'c1' }))

    act(() => result.current.addFiles([file('guide.txt')]))
    await waitFor(() => expect(api.getAttachment).toHaveBeenCalledTimes(1))
    const localId = result.current.attachments[0].localId

    act(() => result.current.remove(localId))
    await new Promise((resolve) => setTimeout(resolve, 250))

    expect(api.getAttachment).toHaveBeenCalledTimes(1)
  })
})
