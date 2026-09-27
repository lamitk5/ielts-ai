import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import LoginPage from '../pages/LoginPage'
import RegisterPage from '../pages/RegisterPage'
import Navbar from '../components/layout/Navbar'
import FloatingTutor from '../components/tutor/FloatingTutor'
import HeroSection from '../components/home/HeroSection'
import { AuthProvider } from '../features/auth/AuthProvider'
import { PreferenceProvider } from '../features/preferences/PreferenceProvider'

function renderAuth(Page) {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <PreferenceProvider>
          <Page />
        </PreferenceProvider>
      </AuthProvider>
    </MemoryRouter>,
  )
}

function renderSettings() {
  localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({
    token: 'test-token',
    user: { id: 'settings-user', email: 'settings@example.com' },
  }))
  return render(
    <MemoryRouter>
      <AuthProvider>
        <Navbar />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('final Academic Luxury restore', () => {
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
    document.documentElement.removeAttribute('data-density')
  })

  test('renders the accessible lamp interaction on login and register', async () => {
    const user = userEvent.setup()
    const { unmount } = renderAuth(LoginPage)
    const pullCord = screen.getByRole('button', { name: 'Bật đèn bàn học' })
    expect(screen.getByTestId('auth-desk-lamp')).toBeInTheDocument()
    expect(screen.getByTestId('auth-form-card')).toHaveAttribute('data-visibility', 'hidden')
    await user.click(pullCord)
    expect(screen.getByRole('button', { name: 'Tắt đèn bàn học' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByTestId('auth-form-card')).toHaveAttribute('data-visibility', 'visible')
    unmount()

    renderAuth(RegisterPage)
    expect(screen.getByRole('button', { name: 'Bật đèn bàn học' })).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /Hiện mật khẩu/ })).toHaveLength(2)
  })

  test('keeps the auth form keyboard reachable while the lamp is off', async () => {
    const user = userEvent.setup()
    renderAuth(LoginPage)
    const pullCord = screen.getByRole('button', { name: 'Bật đèn bàn học' })
    await user.tab()
    expect(document.activeElement).toBe(pullCord)
    await user.tab()
    expect(document.activeElement).toBe(screen.getByLabelText('Email'))
  })

  test('reveals password with a flashlight beam and toggles the accessible label', async () => {
    const user = userEvent.setup()
    renderAuth(LoginPage)
    const password = screen.getByLabelText('Mật khẩu')
    const eye = screen.getByRole('button', { name: 'Hiện mật khẩu' })
    expect(password).toHaveAttribute('type', 'password')
    await user.click(eye)
    expect(password).toHaveAttribute('type', 'text')
    expect(screen.getByTestId('password-flashlight-beam')).toHaveAttribute('data-active', 'true')
    expect(screen.getByRole('button', { name: 'Ẩn mật khẩu' })).toBeInTheDocument()
  })

  test('keeps bookshelf hero and semantic settings controls', async () => {
    render(
      <MemoryRouter>
        <HeroSection />
      </MemoryRouter>,
    )
    expect(screen.getByTestId('hero-bookshelf-background')).toBeInTheDocument()

    const { unmount } = renderSettings()
    await userEvent.setup().click(screen.getByRole('button', { name: 'Cài đặt' }))
    const dialog = screen.getByRole('dialog', { name: 'Cài đặt' })
    expect(within(dialog).queryByText(/slideshow|trình chiếu/i)).not.toBeInTheDocument()
    const density = within(dialog).getByRole('combobox', { name: 'Mật độ hiển thị' })
    expect(within(density).getByRole('option', { name: 'Thoáng' })).toBeInTheDocument()
    expect(within(density).getByRole('option', { name: 'Tiêu chuẩn' })).toBeInTheDocument()
    expect(within(density).getByRole('option', { name: 'Gọn' })).toBeInTheDocument()
    expect(within(dialog).getByRole('combobox', { name: 'Tỷ lệ chia Reading' })).toBeInTheDocument()
    expect(within(dialog).getByRole('combobox', { name: 'Tỷ lệ chia Writing' })).toBeInTheDocument()
    expect(within(dialog).getByRole('checkbox', { name: 'AI gợi ý chủ động' })).toBeInTheDocument()
    await userEvent.setup().selectOptions(density, 'compact')
    expect(document.documentElement).toHaveAttribute('data-density', 'compact')
    unmount()
  })

  test('proactive suggestions are gated without disabling manual Tutor access', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <PreferenceProvider>
          <FloatingTutor />
        </PreferenceProvider>
      </MemoryRouter>,
    )
    await user.click(screen.getByRole('button', { name: 'Mở Trợ giảng AI' }))
    await waitFor(() => expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toBeInTheDocument())
    expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Giải thích lỗi Writing của tôi' })).not.toBeInTheDocument()
  })

  test('runs the mascot blink phase before revealing the Tutor panel', async () => {
    render(
      <MemoryRouter>
        <FloatingTutor />
      </MemoryRouter>,
    )
    const launcher = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    fireEvent.click(launcher)
    expect(screen.queryByRole('dialog', { name: 'Trợ giảng AI' })).not.toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toBeInTheDocument())
    const dialog = screen.getByRole('dialog', { name: 'Trợ giảng AI' })
    expect(dialog.closest('.tutor-panel-layer')).toHaveClass('tutor-panel-layer-launching')
    await waitFor(() => expect(dialog.closest('.tutor-panel-layer')).not.toHaveClass('tutor-panel-layer-launching'))
  })

  test('mascot exposes pupils and changes direction on pointer movement', () => {
    render(
      <MemoryRouter>
        <FloatingTutor />
      </MemoryRouter>,
    )
    const launcher = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    launcher.getBoundingClientRect = () => ({ left: 100, top: 100, width: 80, height: 80 })
    expect(screen.getByTestId('mascot-pupil-left')).toBeInTheDocument()
    fireEvent.pointerMove(launcher, { pointerType: 'mouse', clientX: 175, clientY: 140 })
    return waitFor(() => expect(launcher).toHaveAttribute('data-pointer-direction', 'right'))
  })
})
