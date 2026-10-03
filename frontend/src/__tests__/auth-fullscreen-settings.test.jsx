import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import App from '../App'
import Navbar from '../components/layout/Navbar'
import { AuthProvider } from '../features/auth/AuthProvider'

function renderApp(initialEntry) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <App />
    </MemoryRouter>,
  )
}

function renderAuthenticatedNavbar() {
  localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({
    token: 'settings-token',
    user: { id: 'settings-learner', email: 'settings@example.com' },
  }))
  return render(
    <MemoryRouter>
      <AuthProvider>
        <Navbar />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('auth fullscreen and approved settings panel', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({
      ok: true,
      json: async () => ({
        version: 1,
        themeMode: 'SYSTEM',
        accentPreset: 'GOLD',
        fontScale: 'DEFAULT',
        density: 'DEFAULT',
        reduceMotion: 'SYSTEM',
        proactiveAiEnabled: false,
        crossHighlightEnabled: true,
        timerDefaultEnabled: false,
        readingSplitRatio: 40,
        writingSplitRatio: 40,
      }),
    })))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  test('renders login and register auth shells as full-bleed layouts', () => {
    const { unmount } = renderApp('/login')
    expect(screen.getByRole('region', { name: 'Đăng nhập' })).toHaveClass('auth-full-bleed')
    expect(screen.getByTestId('auth-desk-lamp')).toBeInTheDocument()
    unmount()

    renderApp('/register')
    expect(screen.getByRole('region', { name: 'Tạo tài khoản' })).toHaveClass('auth-full-bleed')
    expect(screen.getAllByRole('button', { name: /Hiện mật khẩu/ })).toHaveLength(2)
  })

  test('exposes profile settings through the account menu without a duplicate icon', async () => {
    const user = userEvent.setup()
    renderAuthenticatedNavbar()
    const navigation = screen.getByRole('navigation', { name: 'Primary navigation' })
    expect(within(navigation).queryByRole('button', { name: 'Cài đặt' })).not.toBeInTheDocument()
    const account = within(navigation).getByRole('button', { name: 'Mở menu tài khoản' })
    await user.click(account)
    expect(screen.getByRole('menuitem', { name: 'Hồ sơ & Cài đặt' })).toHaveAttribute('href', '/profile')
    expect(screen.getByRole('menuitem', { name: 'Bài đã lưu' })).toHaveAttribute('href', '/practice/saved')
  })

  test('keeps the account menu free of a standalone settings action', async () => {
    const user = userEvent.setup()
    renderAuthenticatedNavbar()
    await user.click(screen.getByRole('button', { name: 'Mở menu tài khoản' }))
    expect(screen.queryByRole('menuitem', { name: 'Cài đặt' })).not.toBeInTheDocument()
  })
})
