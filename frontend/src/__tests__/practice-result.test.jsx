import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import PracticeResultPage from '../pages/PracticeResultPage'

beforeEach(() => vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ id: 'attempt-1', status: 'FEEDBACK_READY', skill: 'reading', score: 8, total: 10, practiceVersion: 'v1' }) })))
afterEach(() => vi.unstubAllGlobals())

describe('practice result', () => {
  test('reloads a durable result and exposes a retry/navigation action', async () => {
    render(<MemoryRouter initialEntries={['/practice/results/attempt-1']}><Routes><Route path="/practice/results/:attemptId" element={<PracticeResultPage />} /></Routes></MemoryRouter>)
    expect(await screen.findByText('8/10')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Làm lại/i })).toHaveAttribute('href', '/practice/reading')
  })
})
