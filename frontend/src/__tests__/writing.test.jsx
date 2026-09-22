import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test, vi } from 'vitest'
import App from '../App'

describe('writing assessment route', () => {
  test('exposes a Task 1/2 editor with an explicit estimated-band boundary', () => {
    render(
      <MemoryRouter initialEntries={['/practice/writing']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Writing practice' })).toBeInTheDocument()
    expect(screen.getByLabelText('Bài viết')).toBeInTheDocument()
    expect(screen.getByText(/Band ước lượng/i)).toBeInTheDocument()
  })

  test('shows persisted writing history for an authenticated member', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({
      token: 'member-token',
      user: { id: 'user-1', email: 'student@example.com', firstName: 'Mai', role: 'CUSTOMER' },
    }))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [{ taskId: 'task-1-academic-01', wordCount: 168, status: 'UNAVAILABLE', createdAt: '2026-09-22T10:00:00Z' }],
    }))

    render(
      <MemoryRouter initialEntries={['/practice/writing']}>
        <App />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: 'Lịch sử Writing' })).toBeInTheDocument()
    expect(screen.getByText(/168 từ/i)).toBeInTheDocument()
  })
})
