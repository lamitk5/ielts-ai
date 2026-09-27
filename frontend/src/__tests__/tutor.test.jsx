import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { StrictMode } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import App from '../App'
import FloatingTutor from '../components/tutor/FloatingTutor'

const insufficientAnswer = 'Chưa đủ thông tin để trả lời chắc chắn. Hãy cung cấp câu hỏi, đoạn văn hoặc bài làm liên quan.'

function answeredResponse(answer = 'Gemini trả lời dựa trên câu hỏi của bạn.') {
  return {
    status: 'ANSWERED',
    answer,
    sources: [],
    grounding: { status: 'NOT_ENABLED', ragEnabled: false },
    meta: { requestId: 'test-request' },
    timestamp: '2026-09-22T00:00:00Z',
  }
}

function renderApp(initialEntry = '/') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <App />
    </MemoryRouter>
  )
}

let originalFetch

beforeEach(() => {
  originalFetch = global.fetch
  global.fetch = vi.fn()
})

afterEach(() => {
  global.fetch = originalFetch
  document.body.style.overflow = ''
})

describe('floating AI tutor', () => {
  test('opens, focuses the dialog, closes with Escape, and restores focus', async () => {
    const user = userEvent.setup()
    render(<FloatingTutor />)

    const trigger = screen.getByRole('button', { name: 'Mở Én' })
    await user.click(trigger)

    await waitFor(() => expect(screen.getByRole('dialog', { name: 'Én' })).toBeInTheDocument())
    expect(screen.getByRole('dialog', { name: 'Én' })).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })).toHaveFocus()
    expect(screen.getByRole('button', { name: 'Gửi câu hỏi' })).toHaveClass('tutor-send-button')

    await user.keyboard('{Escape}')

    expect(screen.queryByRole('dialog', { name: 'Én' })).not.toBeInTheDocument()
    expect(trigger).toHaveFocus()
  })

  test('keeps one fullscreen control and one conversation through the homepage Tutor round trip', async () => {
    const user = userEvent.setup()
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({
      token: 'member-token',
      user: { id: 'user-1', email: 'student@example.com', firstName: 'Mai' },
    }))
    renderApp('/')

    await user.click(screen.getByRole('button', { name: 'Mở Én' }))
    await waitFor(() => expect(screen.getByRole('dialog', { name: 'Én' })).toBeInTheDocument())
    const dialog = screen.getByRole('dialog', { name: 'Én' })
    expect(within(dialog).getAllByRole('listitem')).toHaveLength(1)

    await user.click(within(dialog).getByRole('button', { name: 'Toàn màn hình' }))
    expect(dialog).toHaveClass('tutor-shell-fullscreen')
    expect(within(dialog).getAllByRole('button', { name: 'Thu nhỏ' })).toHaveLength(1)

    await user.click(within(dialog).getByRole('button', { name: 'Thu nhỏ' }))
    expect(dialog).not.toHaveClass('tutor-shell-fullscreen')
    await user.click(within(dialog).getByRole('button', { name: 'Đóng Én' }))
    expect(screen.queryByRole('dialog', { name: 'Én' })).not.toBeInTheDocument()
    localStorage.removeItem('ielts-ai-tutor.session')
  })

  test('sends the application contract, shows skeleton loading, and renders a real answer without fake sources', async () => {
    const user = userEvent.setup()
    let resolveRequest
    global.fetch.mockReturnValueOnce(new Promise((resolve) => { resolveRequest = resolve }))
    render(<FloatingTutor />)

    await user.click(screen.getByRole('button', { name: 'Mở Én' }))
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })).toBeInTheDocument())
    const input = screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })
    await user.type(input, 'Giải thích lỗi Writing của tôi')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))

    expect(
      within(screen.getByRole('list', { name: 'Tin nhắn của Én' })).getByText(
        'Giải thích lỗi Writing của tôi',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveAttribute('aria-busy', 'true')
    expect(global.fetch).toHaveBeenCalledWith('/api/ai/chat', expect.objectContaining({ method: 'POST' }))

    const payload = JSON.parse(global.fetch.mock.calls[0][1].body)
    expect(payload.context).toEqual({ skill: 'GENERAL' })
    expect(payload.message).toBe('Giải thích lỗi Writing của tôi')
    expect(payload.history).toEqual([{ role: 'ASSISTANT', content: 'Mình có thể giúp bạn hiểu bài, xem lại lỗi và chọn bước luyện tập tiếp theo.' }])

    resolveRequest({ ok: true, status: 200, json: async () => answeredResponse() })
    await waitFor(() => expect(screen.getByText('Gemini trả lời dựa trên câu hỏi của bạn.')).toBeInTheDocument())
    expect(screen.queryByText('Rubric Writing Task 2')).not.toBeInTheDocument()
    expect(screen.queryByText('Dựa trên nguồn tham chiếu đã kiểm chứng')).not.toBeInTheDocument()
  })

  test('preserves the active practice context for Tutor requests', async () => {
    const user = userEvent.setup()
    global.fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => answeredResponse() })
    render(<FloatingTutor context={{ skill: 'WRITING', exerciseId: 'task-1-academic-01', taskType: 'Task 1 · Academic' }} />)

    await user.click(screen.getByRole('button', { name: 'Mở Én' }))
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })).toBeInTheDocument())
    await user.type(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' }), 'Giải thích Task Achievement')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))

    await waitFor(() => expect(screen.getByText('Gemini trả lời dựa trên câu hỏi của bạn.')).toBeInTheDocument())
    const payload = JSON.parse(global.fetch.mock.calls[0][1].body)
    expect(payload.context).toEqual({ skill: 'WRITING', exerciseId: 'task-1-academic-01', taskType: 'Task 1 · Academic' })
  })

  test('renders the real answer and clears loading when mounted under StrictMode', async () => {
    const user = userEvent.setup()
    let resolveRequest
    global.fetch.mockReturnValueOnce(new Promise((resolve) => { resolveRequest = resolve }))
    render(
      <StrictMode>
        <FloatingTutor />
      </StrictMode>,
    )

    await user.click(screen.getByRole('button', { name: 'Mở Én' }))
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })).toBeInTheDocument())
    await user.type(
      screen.getByRole('textbox', { name: 'Tin nhắn cho Én' }),
      'Giải thích sự khác nhau giữa FALSE và NOT GIVEN trong IELTS Reading.',
    )
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))

    expect(screen.getByRole('status')).toBeInTheDocument()
    resolveRequest({
      ok: true,
      status: 200,
      json: async () => answeredResponse('FALSE mâu thuẫn với thông tin trong bài, còn NOT GIVEN là thông tin không được nêu.'),
    })
    await waitFor(() => expect(screen.getByText('FALSE mâu thuẫn với thông tin trong bài, còn NOT GIVEN là thông tin không được nêu.')).toBeInTheDocument())
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  test('renders every sentence from a long answer without a preview truncation', async () => {
    const user = userEvent.setup()
    const longAnswer = 'Đây là một câu trả lời dài gồm nhiều câu. Nội dung phải được hiển thị đầy đủ. Không được cắt sau vài từ.'
    global.fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => answeredResponse(longAnswer) })
    render(<FloatingTutor />)

    await user.click(screen.getByRole('button', { name: 'Mở Én' }))
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })).toBeInTheDocument())
    await user.type(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' }), 'Câu hỏi dài')
    await user.keyboard('{Enter}')

    await waitFor(() => expect(screen.getByText(longAnswer)).toBeInTheDocument())
    expect(screen.getByText(longAnswer)).toHaveTextContent('Đây là một câu trả lời dài gồm nhiều câu.')
    expect(screen.getByText(longAnswer)).toHaveTextContent('Nội dung phải được hiển thị đầy đủ.')
    expect(screen.getByText(longAnswer)).toHaveTextContent('Không được cắt sau vài từ.')
  })

  test('clears loading and renders a friendly error when the request rejects under StrictMode', async () => {
    const user = userEvent.setup()
    global.fetch.mockRejectedValueOnce(new TypeError('network failure'))
    render(
      <StrictMode>
        <FloatingTutor />
      </StrictMode>,
    )

    await user.click(screen.getByRole('button', { name: 'Mở Én' }))
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })).toBeInTheDocument())
    const input = screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })
    await user.type(input, 'Câu hỏi cần kết nối')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))

    await waitFor(() => expect(screen.getByText('Không thể kết nối tới Én. Vui lòng thử lại.')).toBeInTheDocument())
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    expect(screen.getByText('Câu hỏi cần kết nối')).toBeInTheDocument()
  })

  test('renders insufficient context from the backend without invented citations', async () => {
    const user = userEvent.setup()
    global.fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ ...answeredResponse(insufficientAnswer), status: 'INSUFFICIENT_CONTEXT' }),
    })
    render(<FloatingTutor />)

    await user.click(screen.getByRole('button', { name: 'Mở Én' }))
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })).toBeInTheDocument())
    await user.type(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' }), 'Tại sao câu 14 là FALSE?')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))

    await waitFor(() => expect(screen.getByText(insufficientAnswer)).toBeInTheDocument())
    expect(screen.getByText('Cần thêm ngữ cảnh')).toBeInTheDocument()
    expect(screen.queryByText('Rubric Writing Task 2')).not.toBeInTheDocument()
  })

  test('renders deterministic application data without treating it as a provider error', async () => {
    const user = userEvent.setup()
    global.fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ ...answeredResponse('Bạn đang làm câu Reading 1.'), status: 'APP_DATA' }),
    })
    render(<FloatingTutor context={{ skill: 'READING', questionId: 'reading-q1' }} />)

    await user.click(screen.getByRole('button', { name: 'Mở Én' }))
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })).toBeInTheDocument())
    await user.type(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' }), 'Tôi đang làm câu nào?')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))

    await waitFor(() => expect(screen.getByText('Bạn đang làm câu Reading 1.')).toBeInTheDocument())
    expect(screen.queryByText(/chưa thể trả lời/i)).not.toBeInTheDocument()
  })

  test('offers a provider-neutral retry after a failed request', async () => {
    const user = userEvent.setup()
    global.fetch
      .mockRejectedValueOnce(new TypeError('network failure'))
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => answeredResponse('Retry answer') })
    render(<FloatingTutor />)

    await user.click(screen.getByRole('button', { name: 'Mở Én' }))
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })).toBeInTheDocument())
    await user.type(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' }), 'Thử lại câu hỏi này')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))
    await waitFor(() => expect(screen.getByRole('button', { name: 'Thử lại' })).toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: 'Thử lại' }))
    await waitFor(() => expect(screen.getByText('Retry answer')).toBeInTheDocument())
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  test('shows a friendly rate-limit error and blocks duplicate submits while loading', async () => {
    const user = userEvent.setup()
    let resolveRequest
    global.fetch.mockReturnValueOnce(new Promise((resolve) => { resolveRequest = resolve }))
    render(<FloatingTutor />)

    await user.click(screen.getByRole('button', { name: 'Mở Én' }))
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })).toBeInTheDocument())
    const input = screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })
    await user.type(input, 'Câu hỏi đầu tiên')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))
    expect(screen.getByRole('button', { name: 'Gửi câu hỏi' })).toBeDisabled()
    expect(global.fetch).toHaveBeenCalledTimes(1)

    resolveRequest({
      ok: false,
      status: 429,
      json: async () => ({ error: { code: 'AI_RATE_LIMITED', message: 'internal provider detail' } }),
    })
    await waitFor(() => expect(screen.getByText('Én đang nhận nhiều yêu cầu. Hãy thử lại sau một chút.')).toBeInTheDocument())
    expect(screen.queryByText('internal provider detail')).not.toBeInTheDocument()
  })

  test('submits a keyboard question through the backend contract', async () => {
    const user = userEvent.setup()
    global.fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => answeredResponse('Keyboard answer') })
    render(<FloatingTutor />)

    await user.click(screen.getByRole('button', { name: 'Mở Én' }))
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })).toBeInTheDocument())
    const input = screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })
    await user.type(input, 'How do I improve coherence?')
    await user.keyboard('{Enter}')

    await waitFor(() => expect(screen.getByText('Keyboard answer')).toBeInTheDocument())
    expect(global.fetch).toHaveBeenCalledTimes(1)
  })

  test('keeps the tutor preview and final CTA in the homepage flow', () => {
    renderApp()

    expect(screen.getByRole('region', { name: 'Én' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Sẵn sàng bắt đầu lộ trình IELTS của bạn?' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Bắt đầu đánh giá năng lực' })).toHaveAttribute(
      'href',
      '/assessment',
    )
    expect(screen.getByRole('link', { name: 'Luyện tập 4 kỹ năng' })).toHaveAttribute(
      'href',
      '/#skills',
    )
  })
})
