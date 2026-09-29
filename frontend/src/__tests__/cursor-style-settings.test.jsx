import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { PreferenceProvider } from '../features/preferences/PreferenceProvider'
import SettingsDrawer from '../components/settings/SettingsDrawer'
import CursorStyleLayer from '../components/common/CursorStyleLayer'
import { AuthProvider } from '../features/auth/AuthProvider'

function renderSettings() {
  return render(
    <MemoryRouter>
      <PreferenceProvider>
        <SettingsDrawer open onClose={vi.fn()} openerRef={{ current: null }} />
      </PreferenceProvider>
    </MemoryRouter>,
  )
}

function renderAuthenticatedSettings() {
  localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: { id: 'cursor-user', email: 'cursor@example.com' } }))
  vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({
    ok: true,
    json: async () => ({
      themeMode: 'SYSTEM', accentPreset: 'GOLD', fontScale: 'DEFAULT', density: 'DEFAULT', reduceMotion: 'SYSTEM',
      language: 'VI', proactiveAiEnabled: false, crossHighlightEnabled: true, timerDefaultEnabled: false,
      readingSplitRatio: 40, writingSplitRatio: 40, version: 1,
    }),
  })))
  return render(
    <MemoryRouter>
      <AuthProvider>
        <PreferenceProvider>
          <SettingsDrawer open onClose={vi.fn()} openerRef={{ current: null }} />
        </PreferenceProvider>
      </AuthProvider>
    </MemoryRouter>,
  )
}

function cursorGroup() {
  return within(screen.getByRole('group', { name: 'Giao diện & Hiển thị' }))
}

beforeEach(() => localStorage.clear())
afterEach(() => {
  vi.unstubAllGlobals()
  localStorage.clear()
})

describe('cursor style settings', () => {
  test('renders all five cursor styles with an accessible selected state', () => {
    renderSettings()
    const group = cursorGroup()

    for (const label of ['Mặc định', 'Champagne Gold', 'Scholar Pen', 'Én Feather', 'Pixel Scholar']) {
      expect(group.getByRole('button', { name: label })).toBeInTheDocument()
    }
    expect(group.getByRole('button', { name: 'Mặc định' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText('Kiểu con trỏ')).toBeInTheDocument()
  })

  test.each([
    ['Champagne Gold', 'champagne'],
    ['Scholar Pen', 'scholar-pen'],
    ['Én Feather', 'en-feather'],
    ['Pixel Scholar', 'pixel-scholar'],
  ])('selecting %s applies the cursor preference', async (label, value) => {
    const user = userEvent.setup()
    renderSettings()
    await user.click(cursorGroup().getByRole('button', { name: label }))

    expect(cursorGroup().getByRole('button', { name: label })).toHaveAttribute('aria-pressed', 'true')
    expect(document.documentElement).toHaveAttribute('data-cursor-style', value)
  })

  test('persists cursor style and effects across a fresh render', async () => {
    const user = userEvent.setup()
    const first = renderSettings()
    await user.click(cursorGroup().getByRole('button', { name: 'Scholar Pen' }))
    await user.click(screen.getByRole('checkbox', { name: 'Hiệu ứng con trỏ' }))
    first.unmount()

    renderSettings()
    expect(cursorGroup().getByRole('button', { name: 'Scholar Pen' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('checkbox', { name: 'Hiệu ứng con trỏ' })).not.toBeChecked()
  })

  test('reset defaults restores the system cursor and effect default', async () => {
    const user = userEvent.setup()
    renderSettings()
    await user.click(cursorGroup().getByRole('button', { name: 'Pixel Scholar' }))
    await user.click(screen.getByRole('checkbox', { name: 'Hiệu ứng con trỏ' }))
    await user.click(screen.getByRole('button', { name: 'Khôi phục mặc định' }))
    await user.click(screen.getByRole('button', { name: 'Xác nhận khôi phục' }))

    expect(cursorGroup().getByRole('button', { name: 'Mặc định' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('checkbox', { name: 'Hiệu ứng con trỏ' })).toBeChecked()
    expect(document.documentElement).toHaveAttribute('data-cursor-style', 'default')
  })

  test('animation off and reduced motion disable animated cursor effects but keep the selected style', async () => {
    const user = userEvent.setup()
    renderSettings()
    await user.click(cursorGroup().getByRole('button', { name: 'Én Feather' }))
    await user.click(screen.getByRole('checkbox', { name: 'Cho phép hiệu ứng giao diện (Animation)' }))

    expect(document.documentElement).toHaveAttribute('data-cursor-style', 'en-feather')
    expect(document.documentElement).toHaveAttribute('data-cursor-effects', 'off')

    const motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
    expect(motionQuery).toBeDefined()
  })

  test('prefers-reduced-motion disables cursor effects without changing the selected style', () => {
    vi.stubGlobal('matchMedia', (query) => ({
      matches: query.includes('prefers-reduced-motion'),
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    }))
    renderSettings()

    expect(document.documentElement).toHaveAttribute('data-cursor-style', 'default')
    expect(document.documentElement).toHaveAttribute('data-cursor-effects', 'off')
  })

  test('coarse pointer mode does not activate the animated cursor layer', () => {
    vi.stubGlobal('matchMedia', (query) => ({
      matches: query.includes('(pointer: coarse)'),
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    }))
    renderSettings()

    expect(document.documentElement).toHaveAttribute('data-cursor-pointer', 'coarse')
    expect(document.documentElement).toHaveAttribute('data-cursor-effects', 'off')
  })

  test('authenticated cursor preference survives server hydration without changing the API contract', async () => {
    const user = userEvent.setup()
    renderAuthenticatedSettings()
    await user.click(cursorGroup().getByRole('button', { name: 'Pixel Scholar' }))
    expect(JSON.parse(localStorage.getItem('ielts-ai-tutor.preferences.account.cursor-user.v1')).preferences.cursorStyle).toBe('pixel-scholar')
    expect(JSON.parse(localStorage.getItem('ielts-ai-tutor.preferences.account.cursor-user.v1')).preferences.cursorEffects).toBe(true)
  })

  test('cursor layer updates CSS variables without rerendering on pointer movement', async () => {
    render(<PreferenceProvider><CursorStyleLayer /></PreferenceProvider>)
    const layer = screen.getByTestId('cursor-effect-layer')
    expect(layer).toHaveAttribute('data-active', 'true')
    window.dispatchEvent(new PointerEvent('pointermove', { pointerType: 'mouse', clientX: 120, clientY: 80 }))
    window.dispatchEvent(new PointerEvent('pointermove', { pointerType: 'mouse', clientX: 240, clientY: 160 }))
    await new Promise((resolve) => setTimeout(resolve, 40))
    expect(layer.style.getPropertyValue('--cursor-x')).toBe('240px')
    expect(layer.style.getPropertyValue('--cursor-y')).toBe('160px')
  })
})
