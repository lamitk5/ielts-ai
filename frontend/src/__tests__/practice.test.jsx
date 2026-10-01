import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useNavigate } from 'react-router-dom'
import { afterEach, describe, expect, test, vi } from 'vitest'
import App from '../App'

function RouteControls() {
  const navigate = useNavigate()
  return <div>
    <button type="button" onClick={() => navigate('/practice/reading')}>Đi tới Reading</button>
    <button type="button" onClick={() => navigate('/practice/listening')}>Đi tới Listening</button>
  </div>
}

const renderWithRoutes = () => render(<MemoryRouter initialEntries={['/practice/reading']}><RouteControls /><App /></MemoryRouter>)

const replacementSet = {
  id: 'reading-replacement-02', skill: 'Reading', title: 'Reading replacement', description: 'A replacement set.',
  passage: { title: 'A Different Passage', paragraphs: [{ id: 'reading-replacement-02-p1', text: 'A new passage for a new question.' }] },
  questions: [{ id: 'replacement-q1', prompt: 'What changed in this passage?', options: ['The question', 'The answer'] }],
}

afterEach(() => {
  vi.unstubAllGlobals()
  window.localStorage.clear()
})

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
    if (route.includes('reading')) expect(screen.getByRole('heading', { name: heading }).closest('.practice-page')).toHaveClass('practice-page-editorial')
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
        passage: { title: 'A supported passage', paragraphs: [{ id: 'reading-foundation-01-p1', text: 'Practice improves recall.' }] },
        questions: [{ id: 'reading-q1', prompt: 'What is the main purpose?', options: ['A', 'B'] }],
      }],
    }))

    render(
      <MemoryRouter initialEntries={['/practice/reading']}>
        <App />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: 'What is the main purpose?' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Reading practice' })).toBeInTheDocument()
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
    vi.stubGlobal('fetch', vi.fn(async (url, options = {}) => {
      if (url === '/api/ai/conversations' && !options.method) {
        return { ok: true, status: 200, json: async () => [] }
      }
      if (String(url).startsWith('/api/ai/conversations?') && options.method === 'POST') {
        return { ok: true, status: 201, json: async () => ({ id: 'conversation-practice' }) }
      }
      if (url === '/api/ai/chat' && options.method === 'POST') {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            status: 'ANSWERED',
            answer: 'Hãy kiểm tra từ khóa trong câu hỏi.',
            sources: [],
            grounding: { status: 'NOT_ENABLED', ragEnabled: false },
          }),
        }
      }
      return { ok: true, status: 200, json: async () => [] }
    }))
    window.localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'test-token', user: { id: 'user-1' } }))

    render(
      <MemoryRouter initialEntries={['/practice/reading']}>
        <App />
      </MemoryRouter>,
    )

    await user.click(screen.getByLabelText('B. To improve recall'))
    await user.click(screen.getByRole('button', { name: 'Mở Én' }))
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })).toBeInTheDocument())
    await user.type(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' }), 'Vì sao đáp án này đúng?')
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
  })

  test('submits only selected Reading choices through the existing attempt API', async () => {
    const user = userEvent.setup()
    window.localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'test-token', user: { id: 'user-1' } }))
    vi.stubGlobal('fetch', vi.fn(async (url) => {
      if (url === '/api/practice/reading/attempts') return { ok: true, json: async () => ({ attemptId: 'attempt-1', score: 1, total: 2 }) }
      if (url === '/api/practice/reading/sets') return { ok: true, json: async () => [] }
      return { ok: false, json: async () => null }
    }))
    render(<MemoryRouter initialEntries={['/practice/reading']}><App /></MemoryRouter>)

    await user.click(screen.getByLabelText('B. To improve recall'))
    await user.click(screen.getByRole('button', { name: /câu 2.*chưa trả lời/i }))
    await user.click(screen.getByLabelText('A. More accurate summaries'))
    await user.click(screen.getByRole('button', { name: 'Nộp bài' }))

    await waitFor(() => expect(global.fetch.mock.calls.some(([url]) => url === '/api/practice/reading/attempts')).toBe(true))
    const [, request] = global.fetch.mock.calls.find(([url]) => url === '/api/practice/reading/attempts')
    expect(JSON.parse(request.body)).toEqual({ setId: 'reading-foundation-01', answers: { 'reading-q1': 'B', 'reading-q2': 'A' } })
    expect(await screen.findByText('1/2')).toBeInTheDocument()
  })

  test.each(['empty', 'failed'])('resets Reading state when navigating to a %s Listening set and back', async (mode) => {
    const user = userEvent.setup()
    window.localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'test-token', user: { id: 'user-1' } }))
    vi.stubGlobal('fetch', vi.fn(async (url) => {
      if (url === '/api/practice/reading/sets') return { ok: true, json: async () => [] }
      if (url === '/api/practice/listening/sets') {
        if (mode === 'failed') throw new Error('Network unavailable')
        return { ok: true, json: async () => [] }
      }
      if (url.endsWith('/attempts')) return { ok: true, json: async () => ({ attemptId: 'attempt-1', score: 1, total: 2 }) }
      return { ok: false, json: async () => null }
    }))
    renderWithRoutes()

    await user.click(screen.getByLabelText('B. To improve recall'))
    await user.click(screen.getByRole('button', { name: 'Đánh dấu câu hỏi' }))
    await user.click(screen.getByRole('button', { name: 'Đánh dấu đã xem lại' }))
    await user.click(screen.getByRole('button', { name: 'Nộp bài' }))
    expect(await screen.findByText('1/2')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Đi tới Listening' }))
    expect(screen.getByRole('heading', { name: 'Listening practice' })).toBeInTheDocument()
    expect(screen.getByLabelText('B. 8:00')).not.toBeChecked()
    expect(screen.queryByText('1/2')).not.toBeInTheDocument()
    await user.click(screen.getByLabelText('B. 8:00'))
    await user.click(screen.getByRole('button', { name: 'Nộp bài' }))
    const listeningAttempt = global.fetch.mock.calls.find(([url]) => url === '/api/practice/listening/attempts')
    expect(JSON.parse(listeningAttempt[1].body)).toEqual({ setId: 'listening-foundation-01', answers: { 'listening-q1': 'B' } })

    await user.click(screen.getByRole('button', { name: 'Đi tới Reading' }))
    expect(screen.getByRole('heading', { name: 'Reading practice' })).toBeInTheDocument()
    expect(screen.getByLabelText('B. To improve recall')).not.toBeChecked()
    expect(screen.getByRole('button', { name: /câu 1.*chưa trả lời/i })).not.toHaveTextContent('Đã đánh dấu')
    expect(screen.queryByText('1/2')).not.toBeInTheDocument()
  })

  test('resets answer, review, flag, and result when a different Reading set arrives', async () => {
    const user = userEvent.setup()
    let resolveSetFetch
    window.localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'test-token', user: { id: 'user-1' } }))
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url === '/api/practice/reading/sets') return new Promise((resolve) => { resolveSetFetch = resolve })
      if (url === '/api/practice/reading/attempts') return Promise.resolve({ ok: true, json: async () => ({ attemptId: 'attempt-1', score: 1, total: 2 }) })
      return Promise.resolve({ ok: false, json: async () => null })
    }))
    renderWithRoutes()
    await waitFor(() => expect(resolveSetFetch).toBeDefined())
    await user.click(screen.getByLabelText('B. To improve recall'))
    await user.click(screen.getByRole('button', { name: 'Đánh dấu câu hỏi' }))
    await user.click(screen.getByRole('button', { name: 'Đánh dấu đã xem lại' }))
    await user.click(screen.getByRole('button', { name: 'Nộp bài' }))
    expect(await screen.findByText('1/2')).toBeInTheDocument()

    await act(async () => resolveSetFetch({ ok: true, json: async () => [replacementSet] }))
    expect(await screen.findByRole('heading', { name: 'What changed in this passage?' })).toBeInTheDocument()
    expect(screen.queryByText('1/2')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /câu 1.*chưa trả lời/i })).not.toHaveTextContent('Đã đánh dấu')
    await user.click(screen.getByLabelText('A. The question'))
    await user.click(screen.getByRole('button', { name: 'Nộp bài' }))
    const attempts = global.fetch.mock.calls.filter(([url]) => url === '/api/practice/reading/attempts')
    expect(JSON.parse(attempts[1][1].body)).toEqual({ setId: 'reading-replacement-02', answers: { 'replacement-q1': 'A' } })
  })

  test('reconciles a fetched set with changed question identities under the same set ID', async () => {
    const user = userEvent.setup()
    let resolveSetFetch
    window.localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'test-token', user: { id: 'user-1' } }))
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url === '/api/practice/reading/sets') return new Promise((resolve) => { resolveSetFetch = resolve })
      if (url === '/api/practice/reading/attempts') return Promise.resolve({ ok: true, json: async () => ({ attemptId: 'attempt-2', score: 0, total: 1 }) })
      return Promise.resolve({ ok: false, json: async () => null })
    }))
    renderWithRoutes()
    await user.click(screen.getByLabelText('B. To improve recall'))
    await act(async () => resolveSetFetch({ ok: true, json: async () => [{ ...replacementSet, id: 'reading-foundation-01' }] }))
    expect(await screen.findByRole('heading', { name: 'What changed in this passage?' })).toBeInTheDocument()
    await user.click(screen.getByLabelText('A. The question'))
    await user.click(screen.getByRole('button', { name: 'Nộp bài' }))
    const attempt = global.fetch.mock.calls.find(([url]) => url === '/api/practice/reading/attempts')
    expect(JSON.parse(attempt[1].body)).toEqual({ setId: 'reading-foundation-01', answers: { 'replacement-q1': 'A' } })
  })

  test('ignores a late Reading fetch after navigating to Listening', async () => {
    const user = userEvent.setup()
    let resolveReadingFetch
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url === '/api/practice/reading/sets') return new Promise((resolve) => { resolveReadingFetch = resolve })
      if (url === '/api/practice/listening/sets') return Promise.resolve({ ok: true, json: async () => [] })
      return Promise.resolve({ ok: false, json: async () => null })
    }))
    renderWithRoutes()
    await waitFor(() => expect(resolveReadingFetch).toBeDefined())
    await user.click(screen.getByRole('button', { name: 'Đi tới Listening' }))
    expect(screen.getByRole('heading', { name: 'Listening practice' })).toBeInTheDocument()
    await act(async () => resolveReadingFetch({ ok: true, json: async () => [replacementSet] }))
    expect(screen.getByRole('heading', { name: 'Listening practice' })).toBeInTheDocument()
    expect(screen.queryByText('A Different Passage')).not.toBeInTheDocument()
  })
})
