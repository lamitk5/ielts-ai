import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, test, vi } from 'vitest'
import App from '../App'

const renderReading = () => render(<MemoryRouter initialEntries={['/practice/reading']}><App /></MemoryRouter>)

afterEach(() => {
  vi.unstubAllGlobals()
  window.localStorage.clear()
})

describe('Reading learning workspace', () => {
  test('places the supported Reading passage beside the current question', () => {
    renderReading()

    const passage = screen.getByRole('region', { name: 'Nội dung' })
    const questions = screen.getByRole('region', { name: 'Câu hỏi' })
    expect(passage.compareDocumentPosition(questions) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(within(passage).getByRole('heading', { name: 'Learning Through Spaced Practice' })).toBeInTheDocument()
    expect(passage.querySelector('[data-reading-target-id="reading-foundation-01-p1"]')).toHaveTextContent('improve recall')
    expect(passage.querySelector('[data-reading-target-id="reading-foundation-01-p2"]')).toHaveTextContent('more accurate summaries')
    expect(within(questions).getByRole('heading', { name: /what is its main purpose/i })).toBeInTheDocument()
    expect(within(questions).queryByRole('heading', { name: /which result did the researchers observe/i })).not.toBeInTheDocument()
    expect(passage.querySelector('[data-reading-target-id="reading-foundation-01"]')).toBeInTheDocument()
    expect(questions.querySelector('[data-reading-target-id="reading-q1"]')).toBeInTheDocument()
  })

  test('renders supplied passage paragraphs with stable targets', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => [{
      id: 'reading-foundation-01', skill: 'Reading', title: 'Reading foundation', description: 'A short passage',
      passage: { title: 'Memory and Learning', paragraphs: [{ id: 'p1', text: 'Spaced practice supports recall.' }, { id: 'p2', text: 'Learners revisited material.' }] },
      questions: [{ id: 'reading-q1', prompt: 'What helps recall?', options: ['Practice', 'Speed'] }],
    }] }))
    renderReading()

    expect(await screen.findByRole('heading', { name: 'Memory and Learning' })).toBeInTheDocument()
    const passage = screen.getByRole('region', { name: 'Nội dung' })
    expect(passage.querySelector('[data-reading-target-id="p1"]')).toHaveTextContent('Spaced practice supports recall.')
    expect(passage.querySelector('[data-reading-target-id="p2"]')).toHaveTextContent('Learners revisited material.')
  })

  test('navigates questions, focuses the selected prompt, and keeps answer state', async () => {
    const user = userEvent.setup()
    renderReading()
    await user.click(screen.getByRole('button', { name: /câu 2.*chưa trả lời/i }))
    expect(screen.getByRole('heading', { name: /which result did the researchers observe/i })).toHaveFocus()
    await user.click(screen.getByLabelText('A. More accurate summaries'))
    expect(screen.getByRole('button', { name: /câu 2.*đã trả lời/i })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /câu 1.*chưa trả lời/i }))
    expect(screen.getByRole('heading', { name: /what is its main purpose/i })).toHaveFocus()
    await user.click(screen.getByRole('button', { name: /câu 2.*đã trả lời/i }))
    expect(screen.getByLabelText('A. More accurate summaries')).toBeChecked()
  })

  test('exposes flagged, current, and learner-reviewed states in words', async () => {
    const user = userEvent.setup()
    renderReading()
    const currentQuestion = screen.getByRole('button', { name: /câu 1.*hiện tại.*chưa trả lời/i })
    expect(within(currentQuestion).getByText('Hiện tại')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Đánh dấu câu hỏi' }))
    expect(screen.getByRole('button', { name: /câu 1.*đã đánh dấu/i })).toBeInTheDocument()
    await user.click(screen.getByLabelText('B. To improve recall'))
    expect(screen.getByRole('button', { name: /câu 1.*đã trả lời.*đã đánh dấu/i })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Đánh dấu đã xem lại' }))
    expect(screen.getByRole('button', { name: /câu 1.*đã xem lại/i })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Bỏ đánh dấu câu hỏi' }))
    expect(screen.getByRole('button', { name: /câu 1.*đã xem lại/i })).not.toHaveTextContent('Đã đánh dấu')
  })

  test('persists the Reading 40/60 choice through the existing preference store', async () => {
    const user = userEvent.setup()
    renderReading()
    await user.click(screen.getByRole('button', { name: '60/40' }))
    await waitFor(() => expect(JSON.parse(window.localStorage.getItem('ielts-ai-tutor.preferences.v1')).preferences.readingSplitRatio).toBe(60))
    expect(screen.getByRole('separator', { name: 'Điều chỉnh độ rộng hai khung' })).toHaveAttribute('aria-valuenow', '60')
  })
})
