import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, test, vi } from 'vitest'
import App from '../App'

function renderAuth(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
  localStorage.clear()
})

describe('auth lamp interaction', () => {
  test('uses a tall lamp silhouette with a long vertical body', () => {
    renderAuth('/login')

    const lamp = screen.getByTestId('auth-desk-lamp')
    const [, , viewBoxWidth, viewBoxHeight] = lamp.getAttribute('viewBox').split(' ').map(Number)
    expect(viewBoxHeight).toBeGreaterThan(viewBoxWidth * 1.35)
    expect(lamp.querySelector('.auth-lamp-stem').getAttribute('d')).toContain('V270')
  })

  test('starts Login locked until the lamp is turned on', async () => {
    const user = userEvent.setup()
    renderAuth('/login')

    expect(screen.getByRole('region', { name: 'Đăng nhập' })).toHaveClass('auth-lamp-off')
    expect(screen.getByLabelText('Email')).toBeDisabled()
    expect(screen.getByLabelText('Mật khẩu')).toBeDisabled()
    expect(screen.getByRole('button', { name: /Hiện mật khẩu/ })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Đăng nhập' })).toBeDisabled()
    expect(document.querySelector('.auth-cinematic-content')).toHaveProperty('inert', true)

    await user.click(screen.getByRole('button', { name: 'Bật đèn bàn học' }))

    expect(screen.getByRole('region', { name: 'Đăng nhập' })).toHaveClass('auth-lamp-on')
    expect(screen.getByLabelText('Email')).toBeEnabled()
    expect(screen.getByLabelText('Mật khẩu')).toBeEnabled()
    expect(screen.getByRole('button', { name: /Hiện mật khẩu/ })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Đăng nhập' })).toBeEnabled()
    expect(document.querySelector('.auth-cinematic-content')).toHaveProperty('inert', false)

    await user.click(screen.getByRole('button', { name: 'Tắt đèn bàn học' }))
    expect(screen.getByLabelText('Email')).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Đăng nhập' })).toBeDisabled()
  })

  test('starts Register locked and unlocks every password control with the lamp', async () => {
    const user = userEvent.setup()
    renderAuth('/register')

    expect(screen.getByRole('region', { name: 'Tạo tài khoản' })).toHaveClass('auth-lamp-off')
    expect(screen.getByLabelText('Tên hiển thị')).toBeDisabled()
    expect(screen.getByLabelText('Email')).toBeDisabled()
    expect(screen.getAllByRole('button', { name: /Hiện mật khẩu/ })).toHaveLength(2)
    expect(screen.getAllByRole('button', { name: /Hiện mật khẩu/ }).every((button) => button.disabled)).toBe(true)
    expect(screen.getByRole('button', { name: 'Tạo tài khoản' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Bật đèn bàn học' }))

    expect(screen.getByLabelText('Tên hiển thị')).toBeEnabled()
    expect(screen.getByLabelText('Email')).toBeEnabled()
    expect(screen.getAllByRole('button', { name: /Hiện mật khẩu/ }).every((button) => !button.disabled)).toBe(true)
    expect(screen.getByRole('button', { name: 'Tạo tài khoản' })).toBeEnabled()
  })

  test('keeps the lamp toggle usable when reduced motion is active', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('matchMedia', (query) => ({
      matches: query === '(prefers-reduced-motion: reduce)',
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    }))
    renderAuth('/login')

    await user.click(screen.getByRole('button', { name: 'Bật đèn bàn học' }))

    expect(screen.getByRole('region', { name: 'Đăng nhập' })).toHaveClass('auth-lamp-on')
    expect(screen.getByLabelText('Email')).toBeEnabled()
  })
})
