import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, test, vi } from 'vitest'
import AttachmentComposer from '../components/tutor/AttachmentComposer'
import AttachmentStatus from '../components/tutor/AttachmentStatus'

function files(count, extension = 'txt') {
  return Array.from({ length: count }, (_, index) => new File([`file-${index}`], `file-${index}.${extension}`, { type: 'text/plain' }))
}

describe('Én multimodal picker and cards', () => {
  test('acceptsOneFile', async () => {
    const onFilesSelected = vi.fn()
    render(<AttachmentComposer onFilesSelected={onFilesSelected} />)
    await userEvent.upload(screen.getByLabelText(/chọn tệp tải lên/i), files(1))
    expect(onFilesSelected).toHaveBeenCalledWith(expect.arrayContaining([expect.any(File)]))
  })

  test('acceptsFiveFiles', async () => {
    const onFilesSelected = vi.fn()
    render(<AttachmentComposer onFilesSelected={onFilesSelected} />)
    await userEvent.upload(screen.getByLabelText(/chọn tệp tải lên/i), files(5))
    expect(onFilesSelected.mock.calls[0][0]).toHaveLength(5)
  })

  test('acceptsEverySupportedDocumentAndImageType', async () => {
    const onFilesSelected = vi.fn()
    render(<AttachmentComposer onFilesSelected={onFilesSelected} />)
    const supported = [
      new File(['png'], 'net.png', { type: 'image/png' }),
      new File(['jpg'], 'net.jpg', { type: 'image/jpeg' }),
      new File(['webp'], 'net.webp', { type: 'image/webp' }),
      new File(['txt'], 'net.txt', { type: 'text/plain' }),
      new File(['pdf'], 'net.pdf', { type: 'application/pdf' }),
    ]
    await userEvent.upload(screen.getByLabelText(/chọn tệp tải lên/i), supported)
    expect(onFilesSelected).toHaveBeenCalledWith(expect.arrayContaining(supported))
  })

  test('acceptsDocx', async () => {
    const onFilesSelected = vi.fn()
    render(<AttachmentComposer onFilesSelected={onFilesSelected} />)
    const docx = new File(['docx'], 'net.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    })
    await userEvent.upload(screen.getByLabelText(/chọn tệp tải lên/i), docx)
    expect(onFilesSelected).toHaveBeenCalledWith([docx])
  })

  test('rejectsSixthSelection', async () => {
    const onFilesSelected = vi.fn()
    const onError = vi.fn()
    render(<AttachmentComposer onFilesSelected={onFilesSelected} onError={onError} maxFiles={5} />)
    await userEvent.upload(screen.getByLabelText(/chọn tệp tải lên/i), files(6))
    expect(onFilesSelected).not.toHaveBeenCalled()
    expect(onError).toHaveBeenCalledWith(expect.objectContaining({ code: 'ATTACHMENT_LIMIT_EXCEEDED' }))
  })

  test('rejectsUnsupportedType', async () => {
    const onError = vi.fn()
    render(<AttachmentComposer onFilesSelected={vi.fn()} onError={onError} />)
    fireEvent.change(screen.getByLabelText(/chọn tệp tải lên/i), { target: { files: [new File(['bad'], 'bad.exe')] } })
    expect(onError).toHaveBeenCalledWith(expect.objectContaining({ code: 'ATTACHMENT_TYPE_NOT_SUPPORTED' }))
  })

  test('rejectsOver10MiB', async () => {
    const onError = vi.fn()
    render(<AttachmentComposer onFilesSelected={vi.fn()} onError={onError} />)
    await userEvent.upload(screen.getByLabelText(/chọn tệp tải lên/i), new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'large.txt'))
    expect(onError).toHaveBeenCalledWith(expect.objectContaining({ code: 'ATTACHMENT_SIZE_EXCEEDED' }))
  })

  test('rendersFiveIndependentCards', () => {
    render(<div>{files(5).map((file, index) => <AttachmentStatus key={file.name} attachment={{ id: `a${index}`, filename: file.name, sizeBytes: file.size, status: 'READY' }} onRemove={vi.fn()} />)}</div>)
    expect(screen.getAllByRole('status')).toHaveLength(5)
  })

  test('rendersImageThumbnail', () => {
    render(<AttachmentStatus attachment={{ id: 'image', filename: 'photo.png', sizeBytes: 10, status: 'READY', previewUrl: 'blob:image' }} onRemove={vi.fn()} />)
    expect(screen.getByRole('img', { name: 'photo.png' })).toBeInTheDocument()
  })

  test('rendersDocumentIcon', () => {
    render(<AttachmentStatus attachment={{ id: 'document', filename: 'notes.pdf', sizeBytes: 10, status: 'READY' }} onRemove={vi.fn()} />)
    expect(screen.getByLabelText('Tệp đính kèm: notes.pdf')).toHaveClass('tutor-attachment-document')
  })

  test('exposesKeyboardRemoveRetryActions', async () => {
    const onRemove = vi.fn()
    const onRetry = vi.fn()
    render(<AttachmentStatus attachment={{ id: 'failed', filename: 'notes.txt', sizeBytes: 10, status: 'FAILED' }} onRemove={onRemove} onRetry={onRetry} />)
    const remove = screen.getByRole('button', { name: 'Xóa tệp đính kèm' })
    remove.focus()
    await userEvent.keyboard('{Enter}')
    expect(onRemove).toHaveBeenCalled()
    const retry = screen.getByRole('button', { name: 'Thử lại tải tệp' })
    retry.focus()
    await userEvent.keyboard('{Enter}')
    expect(onRetry).toHaveBeenCalled()
  })
})
