import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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

    const trigger = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    await user.click(trigger)

    expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' })).toHaveFocus()
    expect(screen.getByRole('button', { name: 'Gửi câu hỏi' })).toHaveClass('tutor-send-button')

    await user.keyboard('{Escape}')

    expect(screen.queryByRole('dialog', { name: 'Trợ giảng AI' })).not.toBeInTheDocument()
    expect(trigger).toHaveFocus()
  })

  test('sends the application contract, shows skeleton loading, and renders a real answer without fake sources', async () => {
    const user = userEvent.setup()
    let resolveRequest
    global.fetch.mockReturnValueOnce(new Promise((resolve) => { resolveRequest = resolve }))
    render(<FloatingTutor />)

    await user.click(screen.getByRole('button', { name: 'Mở Trợ giảng AI' }))
    const input = screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' })
    await user.type(input, 'Giải thích lỗi Writing của tôi')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))

    expect(
      within(screen.getByRole('list', { name: 'Tin nhắn Trợ giảng AI' })).getByText(
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

  test('renders insufficient context from the backend without invented citations', async () => {
    const user = userEvent.setup()
    global.fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ ...answeredResponse(insufficientAnswer), status: 'INSUFFICIENT_CONTEXT' }),
    })
    render(<FloatingTutor />)

    await user.click(screen.getByRole('button', { name: 'Mở Trợ giảng AI' }))
    await user.type(screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' }), 'Tại sao câu 14 là FALSE?')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))

    await waitFor(() => expect(screen.getByText(insufficientAnswer)).toBeInTheDocument())
    expect(screen.getByText('Cần thêm ngữ cảnh')).toBeInTheDocument()
    expect(screen.queryByText('Rubric Writing Task 2')).not.toBeInTheDocument()
  })

  test('shows a friendly rate-limit error and blocks duplicate submits while loading', async () => {
    const user = userEvent.setup()
    let resolveRequest
    global.fetch.mockReturnValueOnce(new Promise((resolve) => { resolveRequest = resolve }))
    render(<FloatingTutor />)

    await user.click(screen.getByRole('button', { name: 'Mở Trợ giảng AI' }))
    const input = screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' })
    await user.type(input, 'Câu hỏi đầu tiên')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))
    expect(screen.getByRole('button', { name: 'Gửi câu hỏi' })).toBeDisabled()
    expect(global.fetch).toHaveBeenCalledTimes(1)

    resolveRequest({
      ok: false,
      status: 429,
      json: async () => ({ error: { code: 'AI_RATE_LIMITED', message: 'internal provider detail' } }),
    })
    await waitFor(() => expect(screen.getByText('Trợ giảng AI đang nhận nhiều yêu cầu. Hãy thử lại sau một chút.')).toBeInTheDocument())
    expect(screen.queryByText('internal provider detail')).not.toBeInTheDocument()
  })

  test('submits a keyboard question through the backend contract', async () => {
    const user = userEvent.setup()
    global.fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => answeredResponse('Keyboard answer') })
    render(<FloatingTutor />)

    await user.click(screen.getByRole('button', { name: 'Mở Trợ giảng AI' }))
    const input = screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' })
    await user.type(input, 'How do I improve coherence?')
    await user.keyboard('{Enter}')

    await waitFor(() => expect(screen.getByText('Keyboard answer')).toBeInTheDocument())
    expect(global.fetch).toHaveBeenCalledTimes(1)
  })

  test('keeps the tutor preview and final CTA in the homepage flow', () => {
    renderApp()

    expect(screen.getByRole('region', { name: 'Trợ giảng AI' })).toBeInTheDocument()
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
