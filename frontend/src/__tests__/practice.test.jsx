import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test, vi } from 'vitest'
import App from '../App'

describe('deterministic practice routes', () => {
  test.each([
    ['/practice/reading', 'Reading practice'],
    ['/practice/listening', 'Listening practice'],
  ])('renders a usable %s shell', (route, heading) => {
    render(
      <MemoryRouter initialEntries={[route]}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: heading })).toBeInTheDocument()
    expect(screen.getByText(/synthetic practice set/i)).toBeInTheDocument()
  })

  test('states the safe audio boundary for the synthetic Listening fixture', () => {
    render(
      <MemoryRouter initialEntries={['/practice/listening']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByText(/phát audio chưa được cấu hình/i)).toBeInTheDocument()
  })

  test('normalizes the backend practice-set shape before rendering', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [{
        id: 'reading-foundation-01',
        skill: 'Reading',
        title: 'Reading foundation',
        description: 'Một bộ đề synthetic.',
        questions: [{ id: 'reading-q1', prompt: 'What is the main purpose?', options: ['A', 'B'] }],
      }],
    }))

    render(
      <MemoryRouter initialEntries={['/practice/reading']}>
        <App />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: 'Reading practice' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'What is the main purpose?' })).toBeInTheDocument()
  })

  test('unknown skill keeps a safe fallback', () => {
    render(
      <MemoryRouter initialEntries={['/practice/unknown']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: /Practice area unavailable/ })).toBeInTheDocument()
  })

  test('Tutor receives stable practice references without the answer key', async () => {
    const user = userEvent.setup()
    const originalFetch = global.fetch
    global.fetch = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => [] })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          status: 'ANSWERED',
          answer: 'Hãy kiểm tra từ khóa trong câu hỏi.',
          sources: [],
          grounding: { status: 'NOT_ENABLED', ragEnabled: false },
        }),
      })
    window.localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'test-token', user: { id: 'user-1' } }))

    render(
      <MemoryRouter initialEntries={['/practice/reading']}>
        <App />
      </MemoryRouter>,
    )

    await user.click(screen.getByLabelText('B. To improve recall'))
    await user.click(screen.getByRole('button', { name: 'Mở Trợ giảng AI' }))
    await user.type(screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' }), 'Vì sao đáp án này đúng?')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))

    const tutorCall = global.fetch.mock.calls.find(([url]) => url === '/api/ai/chat')
    expect(tutorCall).toBeDefined()
    const payload = JSON.parse(tutorCall[1].body)
    expect(payload.context).toMatchObject({
      skill: 'READING',
      lessonId: 'reading-foundation-01',
      exerciseId: 'reading-foundation-01',
      questionId: 'reading-q1',
    })
    expect(payload.context).not.toHaveProperty('answerKey')
    global.fetch = originalFetch
  })
})
