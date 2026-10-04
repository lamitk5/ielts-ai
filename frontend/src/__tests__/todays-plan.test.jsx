import { describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import TodaysPlanSection from '../components/learning/TodaysPlanSection'

describe('today plan', () => {
  it('caps visible recommendations and shows their evidence reason', async () => {
    const items = Array.from({ length: 7 }, (_, index) => ({ id: String(index), skill: 'READING', title: `Bài ${index}`, durationMinutes: 10, targeted: true, reason: 'Dựa trên dữ liệu bài đã nộp.', route: '/practice/reading' }))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ items }) }))
    render(<MemoryRouter><TodaysPlanSection /></MemoryRouter>)
    await waitFor(() => expect(screen.getByText('Hôm nay học gì?')).toBeInTheDocument())
    expect(screen.getAllByText(/Dựa trên dữ liệu/)).toHaveLength(5)
  })
})
