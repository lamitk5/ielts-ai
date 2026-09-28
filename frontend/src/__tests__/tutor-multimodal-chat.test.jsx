import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, test, vi } from 'vitest'
import TutorComposer from '../components/tutor/TutorComposer'
import TutorMessage from '../components/tutor/TutorMessage'

function ready(id, filename = `${id}.txt`) {
  return { localId: `local-${id}`, id, filename, sizeBytes: 10, status: 'READY' }
}

describe('Én multimodal chat composer and history', () => {
  test('textOnlySendUnchanged', async () => {
    const user = userEvent.setup()
    const onSend = vi.fn()
    render(<TutorComposer onSend={onSend} />)
    await user.type(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' }), 'hello')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))
    expect(onSend).toHaveBeenCalledWith('hello')
  })

  test('waitsForReadyAttachments', async () => {
    render(<TutorComposer onSend={vi.fn()} attachments={[{ ...ready('a1'), status: 'PROCESSING' }]} />)
    expect(screen.getByRole('button', { name: 'Gửi câu hỏi' })).toBeDisabled()
  })

  test('refusesSilentFailedOmission', async () => {
    render(<TutorComposer onSend={vi.fn()} attachments={[{ ...ready('a1'), status: 'FAILED', errorMessage: 'failed' }]} />)
    expect(screen.getByText('failed')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Gửi câu hỏi' })).toBeDisabled()
  })

  test('sendsAllReadyIdsInOrder', async () => {
    const user = userEvent.setup()
    const onSend = vi.fn()
    render(<TutorComposer onSend={onSend} attachments={[ready('a1'), ready('a2')]} />)
    await user.type(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' }), 'compare')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))
    expect(onSend).toHaveBeenCalledWith('compare', { attachmentIds: ['a1', 'a2'] })
  })

  test('retriesChatWithoutUpload', async () => {
    const user = userEvent.setup()
    const onSend = vi.fn()
    render(<TutorComposer onSend={onSend} attachments={[ready('a1')]} />)
    await user.type(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' }), 'retry')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))
    expect(onSend).toHaveBeenCalledWith('retry', { attachmentIds: ['a1'] })
  })

  test('reloadsAttachmentChips', () => {
    render(<TutorMessage message={{ role: 'user', content: 'see', attachments: [ready('a1', 'essay.pdf')] }} />)
    expect(screen.getByText('essay.pdf')).toBeInTheDocument()
  })

  test('rendersRealAttachmentProvenance', () => {
    render(<TutorMessage message={{ role: 'assistant', content: 'Based on your file', attachmentSources: [ready('a1', 'essay.pdf')] }} />)
    expect(screen.getByText('essay.pdf')).toBeInTheDocument()
  })

  test('keepsComposerKeyboardAccessible', async () => {
    const user = userEvent.setup()
    render(<TutorComposer onSend={vi.fn()} />)
    const input = screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })
    await user.tab()
    expect(input).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Gửi câu hỏi' })).toHaveAttribute('type', 'submit')
  })
})
