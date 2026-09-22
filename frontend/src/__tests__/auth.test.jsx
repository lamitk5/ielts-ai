import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test, vi } from 'vitest'
import App from '../App'

describe('authentication foundation', () => {
  test('login page exposes accessible email and password fields', () => {
    render(
      <MemoryRouter initialEntries={['/login']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Đăng nhập' })).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveAttribute('type', 'email')
    expect(screen.getByLabelText('Mật khẩu')).toHaveAttribute('type', 'password')
  })

  test('login submits normalized auth response and redirects home', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        token: 'opaque-token',
        user: { id: 'user-1', email: 'student@example.com', firstName: 'Mai', role: 'CUSTOMER' },
      }),
    }))

    render(
      <MemoryRouter initialEntries={['/login']}>
        <App />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText('Email'), 'student@example.com')
    await user.type(screen.getByLabelText('Mật khẩu'), 'password-123')
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    expect(await screen.findByRole('heading', { name: 'Bứt phá Band điểm IELTS cùng Trợ giảng AI Độc quyền' })).toBeInTheDocument()
    expect(localStorage.getItem('ielts-ai-tutor.session')).toContain('opaque-token')
  })

  test('authenticated navigation exposes logout and real progress anchor', async () => {
    const user = userEvent.setup()
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({
      token: 'member-token',
      user: { id: 'user-1', email: 'student@example.com', firstName: 'Mai', role: 'CUSTOMER' },
    }))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, status: 204, json: async () => null }))

    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('button', { name: 'Đăng xuất' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Đăng nhập' })).not.toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: 'Tiến độ' }).every((link) => link.getAttribute('href') === '/#progress')).toBe(true)

    await user.click(screen.getByRole('button', { name: 'Đăng xuất' }))

    await waitFor(() => expect(screen.getByRole('link', { name: 'Đăng nhập' })).toBeInTheDocument())
    expect(localStorage.getItem('ielts-ai-tutor.session')).toBeNull()
  })
})
