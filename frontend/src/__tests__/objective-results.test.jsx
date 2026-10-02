import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import PracticeResultPage from '../pages/PracticeResultPage'

beforeEach(() => vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
  ok: true,
  json: async () => ({
    id: 'submission-1', skill: 'READING', status: 'GRADED', score: 1, total: 2, accuracy: 50,
    questionResults: [{ questionId: 'q1', learnerAnswer: 'B', correctAnswer: 'A', correct: false, explanation: 'Đọc lại đoạn dẫn chứng.' }],
  }),
})))
afterEach(() => vi.unstubAllGlobals())

describe('objective result review', () => {
  test('renders trusted graded score, question evidence, and accessible next actions', async () => {
    render(<MemoryRouter initialEntries={['/practice/results/submission-1']}><Routes><Route path="/practice/results/:attemptId" element={<PracticeResultPage />} /></Routes></MemoryRouter>)
    expect(await screen.findByText('1/2')).toBeInTheDocument()
    expect(screen.getByText('Đáp án đúng:')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Hỏi Én/i })).toHaveAttribute('href', '/tutor')
  })

  test('does not present a score while result is processing', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => ({ id: 's1', skill: 'LISTENING', status: 'SCORING', questionResults: [] }) })
    render(<MemoryRouter initialEntries={['/practice/results/s1']}><Routes><Route path="/practice/results/:attemptId" element={<PracticeResultPage />} /></Routes></MemoryRouter>)
    expect(await screen.findByText('Đang xử lý kết quả')).toBeInTheDocument()
    expect(screen.queryByText(/\d+\/\d+/)).not.toBeInTheDocument()
  })
})
