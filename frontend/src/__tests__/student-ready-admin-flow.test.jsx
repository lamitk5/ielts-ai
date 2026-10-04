import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, test, vi } from 'vitest'
import App from '../App'

afterEach(() => { localStorage.clear(); vi.restoreAllMocks() })

describe('student-ready admin flow', () => {
  test('admin can enter the practice generator shell after authentication', () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'admin-token', user: { id: 'admin-1', role: 'ADMIN', email: 'admin@example.com' } }))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => [] }))

    render(<MemoryRouter initialEntries={['/admin/practice-generator']}><App /></MemoryRouter>)

    expect(screen.getByRole('heading', { name: 'Trình tạo bài luyện bằng AI' })).toBeInTheDocument()
  })
})
