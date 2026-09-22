import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test, vi } from 'vitest'
import App from '../App'

describe('speaking practice boundary', () => {
  test('renders prompt flow and explicit unavailable transcription state', () => {
    render(
      <MemoryRouter initialEntries={['/practice/speaking']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Speaking practice' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Lưu câu trả lời' })).toBeInTheDocument()
    expect(screen.getByText(/STT chưa được cấu hình/i)).toBeInTheDocument()
  })

  test('shows persisted speaking history for an authenticated member', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({
      token: 'member-token',
      user: { id: 'user-1', email: 'student@example.com', firstName: 'Mai', role: 'CUSTOMER' },
    }))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [{ promptId: 'speaking-p1-01', status: 'STT_NOT_CONFIGURED', createdAt: '2026-09-22T10:00:00Z' }],
    }))

    render(
      <MemoryRouter initialEntries={['/practice/speaking']}>
        <App />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: 'Lịch sử Speaking' })).toBeInTheDocument()
    expect(screen.getByText(/STT_NOT_CONFIGURED/i)).toBeInTheDocument()
  })
})
