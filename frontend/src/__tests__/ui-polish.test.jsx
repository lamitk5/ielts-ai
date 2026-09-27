import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, test, vi } from 'vitest'
import App from '../App'
import FloatingTutor from '../components/tutor/FloatingTutor'
import { PreferenceProvider } from '../features/preferences/PreferenceProvider'
import { writeGuestPreferences } from '../features/preferences/preferenceStorage'

function renderTutor() {
  return render(
    <MemoryRouter>
      <PreferenceProvider><FloatingTutor /></PreferenceProvider>
    </MemoryRouter>,
  )
}

afterEach(() => { vi.useRealTimers(); localStorage.clear() })

describe('AI Tutor mascot launcher', () => {
  test('renders the premium LUMEN Scholar mascot identity', () => {
    renderTutor()

    const launcher = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    expect(launcher).toHaveAttribute('data-mascot', 'lumen-scholar')
    expect(screen.getByTestId('lumen-scholar-mascot')).toBeInTheDocument()
    expect(screen.getByTestId('lumen-scholar-mascot')).toHaveClass('ai-tutor-mascot')
    expect(screen.getByTestId('lumen-scholar-mascot').querySelector('.ai-tutor-mascot-cap')).toBeInTheDocument()
    expect(screen.getByTestId('lumen-scholar-mascot').querySelector('.ai-tutor-mascot-eye-left')).toBeInTheDocument()
    expect(screen.getByTestId('lumen-scholar-mascot').querySelector('.ai-tutor-mascot-eye-right')).toBeInTheDocument()
    expect(screen.getByTestId('lumen-scholar-mascot').querySelector('.ai-tutor-mascot-ear')).not.toBeInTheDocument()
    expect(screen.getByText('Trợ giảng AI')).toHaveClass('ai-tutor-mascot-tooltip')
  })

  test('replaces the legacy launcher with an accessible mascot button and tooltip', () => {
    renderTutor()

    const launcher = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    expect(launcher).toHaveClass('ai-tutor-mascot-launcher')
    expect(launcher).toHaveAttribute('data-pointer-direction', 'center')
    expect(screen.getByText('Trợ giảng AI')).toHaveClass('ai-tutor-mascot-tooltip')
    expect(screen.queryByTestId('floating-tutor-button')).not.toBeInTheDocument()
  })

  test('shows the blink reaction before opening Tutor on click', () => {
    vi.useFakeTimers()
    renderTutor()

    const launcher = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    fireEvent.click(launcher)
    expect(launcher).toHaveClass('ai-tutor-mascot-launcher-activating')
    expect(screen.queryByRole('dialog', { name: 'Trợ giảng AI' })).not.toBeInTheDocument()

    act(() => vi.advanceTimersByTime(259))
    expect(screen.queryByRole('dialog', { name: 'Trợ giảng AI' })).not.toBeInTheDocument()
    act(() => vi.advanceTimersByTime(1))
    expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toBeInTheDocument()
    vi.useRealTimers()
  })

  test('opens the existing Tutor on click, Enter, and Space without a duplicate launcher', async () => {
    const user = userEvent.setup()
    renderTutor()

    const launcher = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    await user.click(launcher)
    await waitFor(() => expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: 'Mở Trợ giảng AI' })).not.toBeInTheDocument()

    await user.click(within(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).getByRole('button', { name: 'Đóng Trợ giảng AI' }))
    const restoredLauncher = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    restoredLauncher.focus()
    await user.keyboard('{Enter}')
    await waitFor(() => expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toBeInTheDocument())

    await user.click(within(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).getByRole('button', { name: 'Đóng Trợ giảng AI' }))
    screen.getByRole('button', { name: 'Mở Trợ giảng AI' }).focus()
    await user.keyboard(' ')
    await waitFor(() => expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toBeInTheDocument())
  })

  test('tracks a pointer direction for fine pointers and remains keyboard-safe', async () => {
    renderTutor()
    const launcher = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    launcher.getBoundingClientRect = () => ({ left: 100, top: 100, width: 80, height: 80 })

    fireEvent.pointerMove(launcher, { pointerType: 'mouse', clientX: 200, clientY: 140 })
    await waitFor(() => expect(launcher).toHaveAttribute('data-pointer-direction', 'right'))
    expect(launcher.style.getPropertyValue('--mascot-pupil-x')).toBe('1.2px')
    fireEvent.pointerLeave(launcher, { pointerType: 'mouse' })
    expect(launcher).toHaveAttribute('data-pointer-direction', 'center')
    expect(launcher.style.getPropertyValue('--mascot-pupil-x')).toBe('0px')
    expect(launcher).toHaveClass('ai-tutor-mascot-launcher')
  })

  test('exposes idle motion only when reduced motion is not requested', async () => {
    const { unmount } = renderTutor()
    expect(screen.getByTestId('lumen-scholar-mascot')).toHaveAttribute('data-idle-motion', 'enabled')
    unmount()
    writeGuestPreferences({ reduceMotion: 'reduce' })
    renderTutor()
    expect(screen.getByTestId('lumen-scholar-mascot')).toHaveAttribute('data-idle-motion', 'disabled')
  })

  test('keeps the mascot usable with reduced motion and ignores touch tracking', async () => {
    const user = userEvent.setup()
    writeGuestPreferences({ reduceMotion: 'reduce' })
    renderTutor()
    const launcher = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    fireEvent.pointerMove(launcher, { pointerType: 'touch', clientX: 999, clientY: 999 })
    expect(launcher).toHaveAttribute('data-pointer-direction', 'center')
    await user.click(launcher)
    expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toBeInTheDocument()
  })
})

describe('premium UI polish', () => {
  test('keeps hero content and marks the visual as enlarged without replacing its visual concept', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Bứt phá Band điểm IELTS cùng Trợ giảng AI Độc quyền' })).toBeInTheDocument()
    expect(screen.getByRole('search')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Làm bài Test đánh giá năng lực' })).toBeInTheDocument()
    expect(screen.getByTestId('hero-visual')).toHaveClass('hero-visual-large')
    expect(screen.getByTestId('hero-visual')).toHaveAttribute('data-hero-visual', 'learning-intelligence')
  })

  test('retains navigation and transitions the header into a stronger glass state on scroll', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    )

    const header = screen.getByRole('banner')
    expect(header).toHaveClass('site-header-glass')
    expect(within(screen.getByRole('navigation', { name: 'Primary navigation' })).getByRole('link', { name: 'Trang chủ' })).toHaveAttribute('href', '/')
    expect(screen.getByRole('button', { name: /open navigation menu/i })).toBeInTheDocument()

    Object.defineProperty(window, 'scrollY', { configurable: true, value: 120 })
    fireEvent.scroll(window)
    expect(header).toHaveClass('site-header-scrolled')
  })
})
