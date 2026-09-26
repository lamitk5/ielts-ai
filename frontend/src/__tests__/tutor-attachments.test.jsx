import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import AttachmentComposer from '../components/tutor/AttachmentComposer'
import AttachmentStatus from '../components/tutor/AttachmentStatus'
import TutorComposer from '../components/tutor/TutorComposer'
import {
  ATTACHMENT_LIMITS,
  ATTACHMENT_STATUS,
  deleteAttachment,
  getAttachment,
  uploadAttachment,
  validateAttachmentFile,
} from '../services/tutorAttachmentsApi'

describe('bounded Tutor attachments client validation & API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('fetch', vi.fn())
  })

  test('validates and accepts PDF, DOCX, and TXT files under 10 MiB', () => {
    const validPdf = new File(['%PDF-1.4 dummy'], 'essay.pdf', { type: 'application/pdf' })
    const validDocx = new File(['docx content'], 'notes.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    })
    const validTxt = new File(['text content'], 'outline.txt', { type: 'text/plain' })

    expect(validateAttachmentFile(validPdf)).toEqual({ valid: true, error: null })
    expect(validateAttachmentFile(validDocx)).toEqual({ valid: true, error: null })
    expect(validateAttachmentFile(validTxt)).toEqual({ valid: true, error: null })
  })

  test('rejects files larger than 10 MiB boundary', () => {
    const oversizedBytes = new Uint8Array(10 * 1024 * 1024 + 1)
    const oversizedFile = new File([oversizedBytes], 'large.pdf', { type: 'application/pdf' })

    const result = validateAttachmentFile(oversizedFile)
    expect(result.valid).toBe(false)
    expect(result.error).toMatchObject({
      code: 'ATTACHMENT_SIZE_EXCEEDED',
      message: expect.stringContaining('10MB'),
    })
  })

  test('rejects HTML, executables, scripts, and other unapproved formats', () => {
    const htmlFile = new File(['<h1>Malicious</h1>'], 'attack.html', { type: 'text/html' })
    const exeFile = new File(['MZ binary'], 'virus.exe', { type: 'application/x-msdownload' })
    const jsFile = new File(['alert(1)'], 'script.js', { type: 'application/javascript' })

    for (const file of [htmlFile, exeFile, jsFile]) {
      const result = validateAttachmentFile(file)
      expect(result.valid).toBe(false)
      expect(result.error).toMatchObject({
        code: 'ATTACHMENT_TYPE_NOT_SUPPORTED',
        message: expect.stringContaining('PDF, DOCX hoặc TXT'),
      })
    }
  })

  test('uploadAttachment sends multipart request with session token and normalizes success', async () => {
    localStorage.setItem(
      'ielts-ai-tutor.session',
      JSON.stringify({ token: 'test-token', user: { id: 'u1' } }),
    )

    global.fetch.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({
        id: 'att-123',
        filename: 'essay.pdf',
        contentType: 'application/pdf',
        sizeBytes: 2048,
        status: 'READY',
      }),
    })

    const file = new File(['content'], 'essay.pdf', { type: 'application/pdf' })
    const result = await uploadAttachment(file)

    expect(global.fetch).toHaveBeenCalledWith(
      '/api/ai/attachments',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          Authorization: 'Bearer test-token',
        }),
      }),
    )
    expect(result).toMatchObject({
      id: 'att-123',
      filename: 'essay.pdf',
      status: ATTACHMENT_STATUS?.READY ?? 'READY',
    })
  })

  test('deleteAttachment sends authenticated DELETE request', async () => {
    localStorage.setItem(
      'ielts-ai-tutor.session',
      JSON.stringify({ token: 'test-token', user: { id: 'u1' } }),
    )

    global.fetch.mockResolvedValueOnce({
      ok: true,
      status: 204,
    })

    await deleteAttachment('att-123')
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/ai/attachments/att-123',
      expect.objectContaining({
        method: 'DELETE',
        headers: expect.objectContaining({
          Authorization: 'Bearer test-token',
        }),
      }),
    )
  })
})

describe('AttachmentStatus component', () => {
  test('renders filename, formatted size, and status badge', () => {
    const attachment = {
      id: 'att-1',
      filename: 'writing_task2.pdf',
      sizeBytes: 2 * 1024 * 1024,
      status: 'READY',
    }

    render(<AttachmentStatus attachment={attachment} onRemove={vi.fn()} />)

    expect(screen.getByText('writing_task2.pdf')).toBeInTheDocument()
    expect(screen.getByText(/2(\.0)? MB/i)).toBeInTheDocument()
    expect(screen.getByText('Sẵn sàng')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Xóa tệp đính kèm' })).toBeInTheDocument()
  })

  test('calls onRemove when remove button is clicked', async () => {
    const user = userEvent.setup()
    const onRemove = vi.fn()
    const attachment = {
      id: 'att-1',
      filename: 'sample.txt',
      sizeBytes: 1024,
      status: 'READY',
    }

    render(<AttachmentStatus attachment={attachment} onRemove={onRemove} />)
    await user.click(screen.getByRole('button', { name: 'Xóa tệp đính kèm' }))
    expect(onRemove).toHaveBeenCalledTimes(1)
  })

  test('renders retry button and error message when status is FAILED', async () => {
    const user = userEvent.setup()
    const onRetry = vi.fn()
    const attachment = {
      id: 'att-1',
      filename: 'failed.docx',
      sizeBytes: 1024,
      status: 'FAILED',
      errorMessage: 'Tải tệp thất bại. Vui lòng thử lại.',
    }

    render(<AttachmentStatus attachment={attachment} onRetry={onRetry} onRemove={vi.fn()} />)
    expect(screen.getByText('Tải tệp thất bại. Vui lòng thử lại.')).toBeInTheDocument()

    const retryBtn = screen.getByRole('button', { name: 'Thử lại tải tệp' })
    await user.click(retryBtn)
    expect(onRetry).toHaveBeenCalledTimes(1)
  })
})

describe('AttachmentComposer and TutorComposer integration', () => {
  test('AttachmentComposer allows selecting a valid file and rejects invalid files', async () => {
    const user = userEvent.setup()
    const onFileSelected = vi.fn()
    const onError = vi.fn()

    render(
      <AttachmentComposer
        onFileSelected={onFileSelected}
        onError={onError}
        disabled={false}
      />,
    )

    const input = screen.getByLabelText(/chọn tệp tải lên/i)
    const validFile = new File(['text'], 'essay.pdf', { type: 'application/pdf' })

    await user.upload(input, validFile)
    expect(onFileSelected).toHaveBeenCalledWith(validFile)

    const invalidExe = new File(['exe'], 'app.exe', { type: 'application/x-msdownload' })
    fireEvent.change(input, { target: { files: [invalidExe] } })
    expect(onError).toHaveBeenCalledWith(
      expect.objectContaining({ code: 'ATTACHMENT_TYPE_NOT_SUPPORTED' }),
    )
  })

  test('AttachmentComposer disables file trigger when active attachment exists (1 file limit)', () => {
    render(
      <AttachmentComposer
        onFileSelected={vi.fn()}
        hasActiveAttachment={true}
      />,
    )

    const trigger = screen.getByRole('button', { name: /đính kèm/i })
    expect(trigger).toBeDisabled()
  })

  test('TutorComposer disables send while attachment is UPLOADING or PROCESSING', () => {
    const inProgressAttachment = {
      id: 'att-uploading',
      filename: 'reading.pdf',
      status: 'UPLOADING',
    }

    render(
      <TutorComposer
        onSend={vi.fn()}
        loading={false}
        attachment={inProgressAttachment}
      />,
    )

    const sendBtn = screen.getByRole('button', { name: 'Gửi câu hỏi' })
    expect(sendBtn).toBeDisabled()
  })

  test('TutorComposer passes attachment data on send when attachment is READY', async () => {
    const user = userEvent.setup()
    const onSend = vi.fn()
    const readyAttachment = {
      id: 'att-ready',
      filename: 'reading.pdf',
      status: 'READY',
    }

    render(
      <TutorComposer
        onSend={onSend}
        loading={false}
        attachment={readyAttachment}
      />,
    )

    const input = screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' })
    await user.type(input, 'Nhận xét bài luận giúp tôi')

    const sendBtn = screen.getByRole('button', { name: 'Gửi câu hỏi' })
    expect(sendBtn).not.toBeDisabled()

    await user.click(sendBtn)
    expect(onSend).toHaveBeenCalledWith('Nhận xét bài luận giúp tôi', {
      attachmentId: 'att-ready',
    })
  })
})
