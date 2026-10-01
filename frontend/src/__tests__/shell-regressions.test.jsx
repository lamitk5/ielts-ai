import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, test } from 'vitest'
import App from '../App'
import '../styles/globals.css'

describe('shared shell layout contracts', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  test('keeps the header sticky and skip link fixed for the ambient layer shell', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    )

    expect(getComputedStyle(screen.getByRole('banner')).position).toBe('sticky')
    expect(getComputedStyle(screen.getByRole('link', { name: 'Bỏ qua đến nội dung chính' })).position).toBe('fixed')
  })

  test('keeps the account menu in the header stacking context and interactive', async () => {
    const user = userEvent.setup()
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({
      token: 'member-token',
      user: { id: 'member-1', email: 'member@example.com' },
    }))
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    )

    const accountTrigger = screen.getByRole('button', { name: 'Mở menu tài khoản' })
    await user.click(accountTrigger)
    const menu = screen.getByRole('menu')
    const header = screen.getByRole('banner')

    expect(header.contains(menu)).toBe(true)
    expect(Number.parseInt(getComputedStyle(header).zIndex, 10)).toBeGreaterThan(1)
    expect(getComputedStyle(menu).pointerEvents).not.toBe('none')
    expect(within(menu).getByRole('menuitem', { name: 'Đăng xuất' })).toBeEnabled()
  })
})
