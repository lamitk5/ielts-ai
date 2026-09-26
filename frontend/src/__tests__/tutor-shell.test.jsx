import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import TutorShell, { SHELL_STATES } from '../components/tutor/TutorShell'
import ContextBadge from '../components/tutor/ContextBadge'
import TutorComposer from '../components/tutor/TutorComposer'
import TutorQuickActions from '../components/tutor/TutorQuickActions'
import TimeoutRetry from '../components/tutor/TimeoutRetry'

const sampleMessages = [
  { id: 'm1', role: 'assistant', content: 'Xin chào! Mình có thể giúp gì cho bạn?' },
  { id: 'm2', role: 'user', content: 'Giải thích Task Achievement' },
  { id: 'm3', role: 'assistant', content: 'Task Achievement đánh giá mức độ bạn trả lời đầy đủ yêu cầu của đề bài.' },
]

describe('TutorShell component and sub-components', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('renders in STANDARD state with header, message list, context badge, and composer', () => {
    render(
      <TutorShell
        state={SHELL_STATES.STANDARD}
        messages={sampleMessages}
        context={{ skill: 'WRITING', taskType: 'Task 1 · Academic' }}
        onSend={vi.fn()}
        onClose={vi.fn()}
      />,
    )

    expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toHaveClass('tutor-shell-editorial')
    expect(screen.getByText('Xin chào! Mình có thể giúp gì cho bạn?')).toBeInTheDocument()
    expect(screen.getByText('Task Achievement đánh giá mức độ bạn trả lời đầy đủ yêu cầu của đề bài.')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' }).closest('.tutor-composer-container')).toHaveClass('tutor-composer-viewport-safe')
    expect(screen.getByRole('button', { name: 'Gửi câu hỏi' })).toBeInTheDocument()
    expect(within(screen.getByRole('status')).getByText('WRITING')).toBeInTheDocument()
  })

  test('toggles shell states: fullscreen and standard with single toggle', async () => {
    const user = userEvent.setup()
    const onStateChange = vi.fn()
    const { rerender } = render(
      <TutorShell
        state={SHELL_STATES.STANDARD}
        messages={sampleMessages}
        onStateChange={onStateChange}
        onClose={vi.fn()}
      />,
    )

    const expandBtn = screen.getByRole('button', { name: /toàn màn hình/i })
    expect(expandBtn).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /thu gọn/i })).not.toBeInTheDocument()
    await user.click(expandBtn)
    expect(onStateChange).toHaveBeenCalledWith(SHELL_STATES.FULLSCREEN_DESKTOP)

    rerender(
      <TutorShell
        state={SHELL_STATES.FULLSCREEN_DESKTOP}
        messages={sampleMessages}
        onStateChange={onStateChange}
        onClose={vi.fn()}
      />,
    )
    expect(screen.getByRole('dialog')).toHaveClass('tutor-shell-fullscreen')

    const minimizeBtn = screen.getByRole('button', { name: /thu nhỏ/i })
    await user.click(minimizeBtn)
    expect(onStateChange).toHaveBeenCalledWith(SHELL_STATES.STANDARD)
  })

  test('renders a fullscreen study workspace with a safe context pane and one toggle', () => {
    render(
      <TutorShell
        state={SHELL_STATES.FULLSCREEN_DESKTOP}
        messages={sampleMessages}
        context={{
          skill: 'READING',
          taskType: 'Matching Headings',
          exerciseId: 'reading-42',
          title: 'Không được render tùy ý',
        }}
        onStateChange={vi.fn()}
        onSend={vi.fn()}
        onClose={vi.fn()}
      />,
    )

    expect(screen.getByRole('dialog')).toHaveClass('tutor-shell-fullscreen', 'tutor-shell-viewport-safe')
    expect(screen.getByRole('complementary', { name: 'Ngữ cảnh bài luyện' })).toBeInTheDocument()
    const contextPane = screen.getByRole('complementary', { name: 'Ngữ cảnh bài luyện' })
    expect(within(contextPane).getByText('READING')).toBeInTheDocument()
    expect(within(contextPane).getByText('Matching Headings')).toBeInTheDocument()
    expect(within(contextPane).getByText(/Mã bài: reading-42/)).toBeInTheDocument()
    expect(screen.queryByText('Không được render tùy ý')).not.toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Thu nhỏ' })).toHaveLength(1)
  })

  test('chooses the mobile fullscreen state on narrow viewports', async () => {
    const user = userEvent.setup()
    const onStateChange = vi.fn()
    const matchMedia = vi.spyOn(window, 'matchMedia').mockReturnValue({ matches: true })

    render(
      <TutorShell
        state={SHELL_STATES.STANDARD}
        messages={sampleMessages}
        onStateChange={onStateChange}
        onClose={vi.fn()}
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Toàn màn hình' }))
    expect(onStateChange).toHaveBeenCalledWith(SHELL_STATES.FULLSCREEN_MOBILE)
    matchMedia.mockRestore()
  })

  test('ContextBadge shows practice context and allows switching to general question mode', async () => {
    const user = userEvent.setup()
    const onClearContext = vi.fn()
    render(
      <ContextBadge
        context={{ skill: 'READING', exerciseId: 'passage-1', taskType: 'True / False / Not Given' }}
        onClearContext={onClearContext}
      />,
    )

    expect(screen.getByText(/READING/i)).toBeInTheDocument()
    expect(screen.getByText(/True \/ False \/ Not Given/i)).toBeInTheDocument()

    const generalBtn = screen.getByRole('button', { name: 'Hỏi chung' })
    await user.click(generalBtn)
    expect(onClearContext).toHaveBeenCalledTimes(1)
  })

  test('ContextBadge shows stale context notice when context is marked stale', () => {
    render(
      <ContextBadge
        context={{ skill: 'WRITING', exerciseId: 'draft-old', isStale: true }}
      />,
    )

    expect(screen.getByText(/Ngữ cảnh cũ/i)).toBeInTheDocument()
  })

  test('TutorQuickActions populates composer or sends prompt on click', async () => {
    const user = userEvent.setup()
    const onSelect = vi.fn()
    render(<TutorQuickActions onSelectPrompt={onSelect} />)

    const promptBtn = screen.getByRole('button', { name: 'Giải thích lỗi Writing của tôi' })
    await user.click(promptBtn)
    expect(onSelect).toHaveBeenCalledWith('Giải thích lỗi Writing của tôi')
  })

  test('TutorQuickActions renders only valid prompt definitions', async () => {
    const user = userEvent.setup()
    const onSelect = vi.fn()
    render(
      <TutorQuickActions
        onSelectPrompt={onSelect}
        suggestions={[
          { label: 'Gợi ý hợp lệ', prompt: 'Hỏi về bài này', valid: true },
          { label: 'Gợi ý không hợp lệ', prompt: 'Không được dùng', valid: false },
          { label: 'Thiếu prompt' },
          '',
        ]}
      />,
    )

    expect(screen.getByRole('button', { name: 'Gợi ý hợp lệ' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Gợi ý không hợp lệ' })).not.toBeInTheDocument()
    expect(screen.queryByText('Thiếu prompt')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Gợi ý hợp lệ' }))
    expect(onSelect).toHaveBeenCalledWith('Hỏi về bài này')
  })

  test('TimeoutRetry surfaces timeout with retry and cancel actions', async () => {
    const user = userEvent.setup()
    const onRetry = vi.fn()
    const onCancel = vi.fn()
    render(
      <TimeoutRetry
        message="Không thể kết nối tới Trợ giảng AI. Vui lòng thử lại."
        onRetry={onRetry}
        onCancel={onCancel}
      />,
    )

    expect(screen.getByText('Không thể kết nối tới Trợ giảng AI. Vui lòng thử lại.')).toBeInTheDocument()
    const retryBtn = screen.getByRole('button', { name: 'Thử lại' })
    await user.click(retryBtn)
    expect(onRetry).toHaveBeenCalledTimes(1)

    const cancelBtn = screen.getByRole('button', { name: 'Hủy' })
    await user.click(cancelBtn)
    expect(onCancel).toHaveBeenCalledTimes(1)
  })

  test('TutorComposer handles typing, submit, and disabled states during request loading', async () => {
    const user = userEvent.setup()
    const onSend = vi.fn()
    const onCancel = vi.fn()
    const { rerender } = render(<TutorComposer onSend={onSend} loading={false} />)

    const input = screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' })
    const submitBtn = screen.getByRole('button', { name: 'Gửi câu hỏi' })

    expect(submitBtn).toBeDisabled()
    await user.type(input, 'Cách viết Introduction?')
    expect(submitBtn).not.toBeDisabled()

    await user.click(submitBtn)
    expect(onSend).toHaveBeenCalledWith('Cách viết Introduction?')
    expect(input).toHaveValue('')

    rerender(<TutorComposer onSend={onSend} loading={true} onCancel={onCancel} />)
    expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' })).toBeDisabled()
    const cancelAction = screen.getByRole('button', { name: 'Hủy' })
    await user.click(cancelAction)
    expect(onCancel).toHaveBeenCalledTimes(1)
  })

  test('closes on Escape key press and invokes onClose', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    render(
      <TutorShell
        state={SHELL_STATES.STANDARD}
        messages={sampleMessages}
        onClose={onClose}
      />,
    )

    await user.keyboard('{Escape}')
    expect(onClose).toHaveBeenCalledTimes(1)
  })
})
