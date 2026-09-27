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

  test('keeps settings icon-only and immediately before the account utility', () => {
    renderAuthenticatedNavbar()
    const navigation = screen.getByRole('navigation', { name: 'Primary navigation' })
    const settings = within(navigation).getByRole('button', { name: 'Cài đặt' })
    const account = within(navigation).getByRole('button', { name: 'Mở menu tài khoản' })
    expect(within(navigation).queryByText('Cài đặt')).not.toBeInTheDocument()
    const controls = [...navigation.querySelectorAll('a, button')]
    expect(controls.indexOf(settings)).toBe(controls.indexOf(account) - 1)
  })

  test('opens the exact three-section settings panel with supported controls', async () => {
    const user = userEvent.setup()
    renderAuthenticatedNavbar()
    await user.click(screen.getByRole('button', { name: 'Cài đặt' }))
    const dialog = screen.getByRole('dialog', { name: 'Cài đặt' })

    for (const title of ['Giao diện & Hiển thị', 'Chuyển động & Trợ năng', 'Không gian học tập & Én']) {
      expect(within(dialog).getByRole('heading', { name: title })).toBeInTheDocument()
    }
    for (const label of ['Tối', 'Sáng', 'Theo hệ thống', 'Gold', 'Sapphire', 'Emerald', 'Burgundy', 'Violet', 'Slate', 'Thoáng', 'Tiêu chuẩn', 'Gọn']) {
      expect(within(dialog).getAllByText(label).length).toBeGreaterThan(0)
    }
    expect(within(dialog).getByRole('checkbox', { name: 'Cho phép hiệu ứng giao diện (Animation)' })).toBeInTheDocument()
    expect(within(dialog).getByRole('slider', { name: 'Cỡ chữ' })).toBeInTheDocument()
    expect(within(dialog).getByRole('combobox', { name: 'Ngôn ngữ' })).toHaveTextContent('Tiếng Việt')
    expect(within(dialog).getByRole('option', { name: 'English' })).toBeInTheDocument()
    expect(within(dialog).getByRole('checkbox', { name: 'Bật gợi ý chủ động từ Én' })).toBeInTheDocument()
    expect(within(dialog).getByRole('checkbox', { name: 'Bật hiệu ứng sáng vùng lỗi sai (Cross-highlighting)' })).toBeInTheDocument()
    expect(within(dialog).getByRole('checkbox', { name: 'Hiển thị đồng hồ đếm ngược' })).toBeInTheDocument()
    expect(within(dialog).getAllByRole('combobox', { name: 'Tỷ lệ chia Reading' })[0].querySelectorAll('option')).toHaveLength(3)
    expect(within(dialog).getAllByRole('combobox', { name: 'Tỷ lệ chia Writing' })[0].querySelectorAll('option')).toHaveLength(3)
    expect(within(dialog).queryByText(/slideshow|trình chiếu/i)).not.toBeInTheDocument()
    expect(within(dialog).getByRole('button', { name: 'Khôi phục mặc định' })).toBeInTheDocument()
  })
})
