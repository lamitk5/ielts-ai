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
import {
  ATTACHMENT_CONTRACT,
  getAttachmentPresentation,
} from '../features/tutor/attachmentContract'

describe('bounded Tutor attachments client validation & API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('fetch', vi.fn())
  })

  test('validates and accepts PDF, DOCX, TXT, and image files under 10 MiB', () => {
    const validPdf = new File(['%PDF-1.4 dummy'], 'essay.pdf', { type: 'application/pdf' })
    const validDocx = new File(['docx content'], 'notes.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    })
    const validTxt = new File(['text content'], 'outline.txt', { type: 'text/plain' })
    const validPng = new File(['png data'], 'chart.png', { type: 'image/png' })
    const validJpg = new File(['jpg data'], 'photo.jpg', { type: 'image/jpeg' })
    const validWebp = new File(['webp data'], 'image.webp', { type: 'image/webp' })

    expect(validateAttachmentFile(validPdf)).toEqual({ valid: true, error: null })
    expect(validateAttachmentFile(validDocx)).toEqual({ valid: true, error: null })
    expect(validateAttachmentFile(validTxt)).toEqual({ valid: true, error: null })
    expect(validateAttachmentFile(validPng)).toEqual({ valid: true, error: null })
    expect(validateAttachmentFile(validJpg)).toEqual({ valid: true, error: null })
    expect(validateAttachmentFile(validWebp)).toEqual({ valid: true, error: null })
  })

  test('exposes one shared contract and presentation metadata for images/documents', () => {
    expect(ATTACHMENT_CONTRACT.maxSizeBytes).toBe(10 * 1024 * 1024)
    expect(ATTACHMENT_CONTRACT.extensions).toEqual(expect.arrayContaining(['.pdf', '.docx', '.txt', '.png', '.jpg', '.jpeg', '.webp']))
    expect(getAttachmentPresentation({ filename: 'chart.webp', contentType: 'image/webp' })).toMatchObject({
      kind: 'image',
      showThumbnail: true,
      capability: 'VISION_NOT_ENABLED',
    })
    expect(getAttachmentPresentation({ filename: 'essay.pdf', contentType: 'application/pdf' })).toMatchObject({
      kind: 'document',
      showThumbnail: false,
      icon: 'file-text',
    })
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
        message: expect.stringContaining('PDF, DOCX, TXT hoặc PNG/JPG/WEBP'),
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
    const result = await uploadAttachment(file, { requestId: 'tutor-request-1' })

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
    const requestBody = global.fetch.mock.calls[0][1].body
    expect(requestBody.get('requestId')).toBe('tutor-request-1')
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

    expect(screen.getByRole('status')).toHaveClass('tutor-attachment-row', 'tutor-attachment-responsive-safe')
    expect(screen.getByRole('status')).toHaveAttribute('aria-live', 'polite')
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

  test('renders image thumbnail when previewUrl is present', () => {
    const attachment = {
      id: 'att-img',
      filename: 'diagram.png',
      sizeBytes: 1024 * 50,
      status: 'READY',
      previewUrl: 'blob:http://localhost/image-blob',
    }

    render(<AttachmentStatus attachment={attachment} onRemove={vi.fn()} />)
    const img = screen.getByRole('img', { name: 'diagram.png' })
    expect(img).toBeInTheDocument()
    expect(img).toHaveAttribute('src', 'blob:http://localhost/image-blob')
  })

  test('renders image-ready state without claiming image analysis is available', () => {
    const attachment = {
      id: 'att-image-ready',
      filename: 'diagram.png',
      sizeBytes: 1024 * 50,
      status: 'IMAGE_READY',
      capability: 'VISION_NOT_ENABLED',
    }

    render(<AttachmentStatus attachment={attachment} onRemove={vi.fn()} />)

    expect(screen.getByText('Ảnh đã sẵn sàng')).toBeInTheDocument()
    expect(screen.getByText('Phân tích hình ảnh chưa được bật')).toBeInTheDocument()
    expect(screen.queryByText(/đã được phân tích|đã nhận xét/i)).not.toBeInTheDocument()
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

  test('TutorComposer does not send an image as if vision analysis were available', async () => {
    const user = userEvent.setup()
    const onSend = vi.fn()

    render(
      <TutorComposer
        onSend={onSend}
        loading={false}
        attachment={{
          id: 'att-image-ready',
          filename: 'diagram.png',
          status: 'IMAGE_READY',
          capability: 'VISION_NOT_ENABLED',
        }}
      />,
    )

    const input = screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' })
    await user.type(input, 'Tệp này dùng được không?')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))

    expect(onSend).toHaveBeenCalledWith('Tệp này dùng được không?')
  })

  test('attachment actions remain keyboard reachable and image preview has alternative text', async () => {
    const user = userEvent.setup()
    const onRemove = vi.fn()
    render(
      <AttachmentStatus
        attachment={{
          id: 'att-image',
          filename: 'chart.webp',
          sizeBytes: 1024,
          status: 'IMAGE_READY',
          previewUrl: 'blob:http://localhost/chart',
        }}
        onRemove={onRemove}
      />,
    )

    expect(screen.getByRole('img', { name: 'chart.webp' })).toBeInTheDocument()
    const remove = screen.getByRole('button', { name: 'Xóa tệp đính kèm' })
    remove.focus()
    expect(remove).toHaveFocus()
    await user.keyboard('{Enter}')
    expect(onRemove).toHaveBeenCalledTimes(1)
  })
})
