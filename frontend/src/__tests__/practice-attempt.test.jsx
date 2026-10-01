import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import PracticeAttemptPage from '../pages/PracticeAttemptPage'

const set = { id: 'reading-1', skill: 'Reading', title: 'Reading set', description: 'Read well', questions: [{ id: 'q1', prompt: 'Choose one', options: ['A', 'B'] }] }
const attempt = { id: 'attempt-1', practiceId: 'reading-1', practiceVersion: 'v1', skill: 'reading', status: 'IN_PROGRESS', answers: {} }

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn().mockImplementation(async (url, options = {}) => {
    if (url.includes('/api/practice/reading/sets/')) return { ok: true, json: async () => set }
    if (url === '/api/attempts' && options.method === 'POST') return { ok: true, json: async () => attempt }
    if (url.endsWith('/answers')) return { ok: true, json: async () => ({ ...attempt, answers: { q1: 'A' } }) }
    if (url.endsWith('/submit')) return { ok: true, json: async () => ({ ...attempt, status: 'FEEDBACK_READY', answers: { q1: 'A' }, score: 1, total: 1 }) }
    return { ok: true, json: async () => attempt }
  }))
})
afterEach(() => { vi.unstubAllGlobals() })

describe('durable practice attempt', () => {
  test('starts, saves an answer, and submits without leaving loading state', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter initialEntries={['/practice/reading/reading-1']}><Routes><Route path="/practice/:skill/:setId" element={<PracticeAttemptPage />} /></Routes></MemoryRouter>)

    await screen.findByText('Choose one')
    await user.click(screen.getByLabelText('A'))
    await waitFor(() => expect(fetch).toHaveBeenCalledWith('/api/attempts/attempt-1/answers', expect.objectContaining({ method: 'PUT' })))
    await user.click(screen.getByRole('button', { name: 'Nộp bài' }))
    await waitFor(() => expect(screen.getByText(/FEEDBACK_READY|Đã nộp/i)).toBeInTheDocument())
  })
})
