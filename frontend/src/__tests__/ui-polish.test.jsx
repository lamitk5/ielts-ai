import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, test, vi } from 'vitest'
import App from '../App'
import HeroVisual from '../components/home/HeroVisual'
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

afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks(); localStorage.clear() })

describe('AI Tutor mascot launcher', () => {
  test('renders the premium LUMEN Pixel Owl identity', () => {
    renderTutor()

    const launcher = screen.getByRole('button', { name: 'Mở Én' })
    expect(launcher).toHaveAttribute('data-mascot', 'lumen-pixel-owl')
    expect(screen.getByTestId('lumen-scholar-mascot')).toBeInTheDocument()
    expect(screen.getByTestId('lumen-scholar-mascot')).toHaveClass('ai-tutor-mascot', 'ai-tutor-pixel-owl-scholar')
    expect(screen.getByTestId('lumen-scholar-mascot')).toHaveAttribute('shape-rendering', 'crispEdges')
    expect(screen.getByTestId('lumen-scholar-mascot')).toHaveAttribute('aria-label', 'Én')
    expect(screen.getByTestId('lumen-scholar-mascot')).toHaveAttribute('data-character', 'pixel-owl-scholar')
    expect(screen.getByTestId('lumen-scholar-mascot')).toHaveAttribute('data-size', 'bounded')
    expect(screen.getByTestId('lumen-scholar-mascot')).toHaveAttribute('data-blink', 'open')
    expect(screen.getByTestId('lumen-scholar-mascot').querySelector('.ai-tutor-mascot-cap')).toBeInTheDocument()
    expect(screen.getByTestId('lumen-scholar-mascot').querySelector('.ai-tutor-mascot-eye-left')).toBeInTheDocument()
    expect(screen.getByTestId('lumen-scholar-mascot').querySelector('.ai-tutor-mascot-eye-right')).toBeInTheDocument()
    expect(screen.getByTestId('lumen-scholar-mascot').querySelectorAll('.ai-tutor-mascot-pupil')).toHaveLength(2)
    expect(screen.getByTestId('lumen-scholar-mascot').querySelector('.ai-tutor-mascot-glasses')).not.toBeInTheDocument()
    expect(screen.getByTestId('lumen-scholar-mascot').querySelector('.ai-tutor-mascot-ear')).not.toBeInTheDocument()
    expect(screen.getByTestId('lumen-scholar-mascot').querySelector('.pixel-owl-face')).toBeInTheDocument()
    expect(screen.getByTestId('lumen-scholar-mascot').querySelector('.pixel-owl-book')).toBeInTheDocument()
    expect(screen.getByTestId('lumen-scholar-mascot').querySelector('.pixel-owl-wing')).toBeInTheDocument()
    expect(screen.getByTestId('lumen-scholar-mascot').querySelector('.pixel-owl-medallion')).toBeInTheDocument()
    expect(screen.getByTestId('lumen-scholar-mascot').querySelector('.pixel-owl-tear')).toBeInTheDocument()
    expect(screen.getByTestId('lumen-scholar-mascot').querySelector('.pixel-owl-thought')).toBeInTheDocument()
    expect(screen.getByText('Én')).toHaveClass('ai-tutor-mascot-tooltip')
  })

  test('replaces the legacy launcher with an accessible mascot button and tooltip', () => {
    renderTutor()

    const launcher = screen.getByRole('button', { name: 'Mở Én' })
    expect(launcher).toHaveClass('ai-tutor-mascot-launcher')
    expect(launcher).toHaveAttribute('data-pointer-direction', 'center')
    expect(launcher).toHaveAttribute('data-mascot', 'lumen-pixel-owl')
    expect(screen.getByText('Én')).toHaveClass('ai-tutor-mascot-tooltip')
    expect(screen.queryByTestId('floating-tutor-button')).not.toBeInTheDocument()
  })

  test('shows the blink reaction before opening Tutor on click', () => {
    vi.useFakeTimers()
    renderTutor()

    const launcher = screen.getByRole('button', { name: 'Mở Én' })
    fireEvent.click(launcher)
    expect(launcher).toHaveClass('ai-tutor-mascot-launcher-activating')
    expect(screen.getByTestId('lumen-scholar-mascot')).toHaveAttribute('data-blink', 'closed')
    expect(screen.queryByRole('dialog', { name: 'Én' })).not.toBeInTheDocument()

    act(() => vi.advanceTimersByTime(259))
    expect(screen.queryByRole('dialog', { name: 'Én' })).not.toBeInTheDocument()
    act(() => vi.advanceTimersByTime(1))
    expect(screen.getByRole('dialog', { name: 'Én' })).toBeInTheDocument()
    vi.useRealTimers()
  })

  test('opens the existing Tutor on click, Enter, and Space without a duplicate launcher', async () => {
    const user = userEvent.setup()
    renderTutor()

    const launcher = screen.getByRole('button', { name: 'Mở Én' })
    await user.click(launcher)
    await waitFor(() => expect(screen.getByRole('dialog', { name: 'Én' })).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: 'Mở Én' })).not.toBeInTheDocument()

    await user.click(within(screen.getByRole('dialog', { name: 'Én' })).getByRole('button', { name: 'Đóng Én' }))
    const restoredLauncher = screen.getByRole('button', { name: 'Mở Én' })
    restoredLauncher.focus()
    await user.keyboard('{Enter}')
    await waitFor(() => expect(screen.getByRole('dialog', { name: 'Én' })).toBeInTheDocument())

    await user.click(within(screen.getByRole('dialog', { name: 'Én' })).getByRole('button', { name: 'Đóng Én' }))
    screen.getByRole('button', { name: 'Mở Én' }).focus()
    await user.keyboard(' ')
    await waitFor(() => expect(screen.getByRole('dialog', { name: 'Én' })).toBeInTheDocument())
  })

  test('tracks a global pointer outside the launcher and clamps visible pupil direction', async () => {
    renderTutor()
    const launcher = screen.getByRole('button', { name: 'Mở Én' })
    const mascot = screen.getByTestId('lumen-scholar-mascot')
    launcher.getBoundingClientRect = () => ({ left: 100, top: 100, width: 80, height: 80 })
    const anchoredTransform = launcher.style.transform

    fireEvent.pointerMove(window, { pointerType: 'mouse', clientX: 0, clientY: 0 })
    await waitFor(() => expect(launcher).toHaveAttribute('data-pointer-direction', 'above'))
    expect(Number.parseFloat(mascot.style.getPropertyValue('--mascot-pupil-x'))).toBeLessThan(0)
    expect(Number.parseFloat(mascot.style.getPropertyValue('--mascot-pupil-y'))).toBeLessThan(0)
    expect(Number.parseFloat(mascot.style.getPropertyValue('--mascot-pupil-x'))).toBeGreaterThanOrEqual(-4)
    expect(Number.parseFloat(mascot.style.getPropertyValue('--mascot-pupil-y'))).toBeGreaterThanOrEqual(-4)

    fireEvent.pointerMove(window, { pointerType: 'mouse', clientX: 999, clientY: 999 })
    await waitFor(() => expect(launcher).toHaveAttribute('data-pointer-direction', 'below'))
    expect(Number.parseFloat(mascot.style.getPropertyValue('--mascot-pupil-x'))).toBeGreaterThan(0)
    expect(Number.parseFloat(mascot.style.getPropertyValue('--mascot-pupil-y'))).toBeGreaterThan(0)
    expect(Number.parseFloat(mascot.style.getPropertyValue('--mascot-pupil-x'))).toBeLessThanOrEqual(4)
    expect(Number.parseFloat(mascot.style.getPropertyValue('--mascot-pupil-y'))).toBeLessThanOrEqual(4)
    expect(launcher.style.transform).toBe(anchoredTransform)
    expect(launcher).toHaveClass('ai-tutor-mascot-launcher')
  })

  test('keeps global tracking disabled for touch pointers', async () => {
    renderTutor()
    const launcher = screen.getByRole('button', { name: 'Mở Én' })
    const mascot = screen.getByTestId('lumen-scholar-mascot')
    launcher.getBoundingClientRect = () => ({ left: 100, top: 100, width: 80, height: 80 })

    fireEvent.pointerMove(window, { pointerType: 'touch', clientX: 0, clientY: 0 })
    await new Promise((resolve) => setTimeout(resolve, 0))
    expect(launcher).toHaveAttribute('data-pointer-direction', 'center')
    expect(mascot.style.getPropertyValue('--mascot-pupil-x')).toBe('')
    expect(mascot.style.getPropertyValue('--mascot-pupil-y')).toBe('')
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
    const launcher = screen.getByRole('button', { name: 'Mở Én' })
    fireEvent.pointerMove(launcher, { pointerType: 'touch', clientX: 999, clientY: 999 })
    expect(launcher).toHaveAttribute('data-pointer-direction', 'center')
    await user.click(launcher)
    expect(screen.getByRole('dialog', { name: 'Én' })).toBeInTheDocument()
  })

  test('keeps functional pupil direction when reduced motion is enabled', async () => {
    writeGuestPreferences({ reduceMotion: 'reduce' })
    renderTutor()

    const launcher = screen.getByRole('button', { name: 'Mở Én' })
    const mascot = screen.getByTestId('lumen-scholar-mascot')
    launcher.getBoundingClientRect = () => ({ left: 100, top: 100, width: 80, height: 80 })

    fireEvent.pointerMove(window, { pointerType: 'mouse', clientX: 0, clientY: 0 })

    await waitFor(() => expect(Number.parseFloat(mascot.style.getPropertyValue('--mascot-pupil-x'))).toBeLessThan(0))
    expect(Number.parseFloat(mascot.style.getPropertyValue('--mascot-pupil-y'))).toBeLessThan(0)
  })

  test('gives the scholar a subtle idle blink without changing its anchored launcher', () => {
    vi.useFakeTimers()
    renderTutor()

    const launcher = screen.getByRole('button', { name: 'Mở Én' })
    const mascot = screen.getByTestId('lumen-scholar-mascot')
    expect(mascot).toHaveAttribute('data-idle-blink', 'open')

    act(() => vi.advanceTimersByTime(5600))

    expect(mascot).toHaveAttribute('data-idle-blink', 'closed')
    expect(launcher.style.transform).toBe('')
  })

  test('rotates through a quiet idle expression without changing the launcher anchor', () => {
    vi.useFakeTimers()
    renderTutor()

    const launcher = screen.getByRole('button', { name: 'Mở Én' })
    const mascot = screen.getByTestId('lumen-scholar-mascot')
    expect(mascot).toHaveAttribute('data-expression', 'neutral')

    act(() => vi.advanceTimersByTime(18000))

    expect(mascot).not.toHaveAttribute('data-expression', 'neutral')
    expect(launcher.style.transform).toBe('')
  })

  test('shows one reminder bubble within the approved 9–12 second cadence', () => {
    vi.useFakeTimers()
    vi.spyOn(Math, 'random').mockReturnValue(0)
    writeGuestPreferences({ proactiveAiEnabled: true })
    renderTutor()

    expect(screen.queryByRole('button', { name: /Mở Én: / })).not.toBeInTheDocument()
    act(() => vi.advanceTimersByTime(9000))

    expect(screen.getByRole('button', { name: 'Mở Én: Học đi bạn ê 👀' })).toBeInTheDocument()

    act(() => vi.advanceTimersByTime(5000))
    expect(screen.queryByRole('button', { name: /Mở Én: / })).not.toBeInTheDocument()

    act(() => vi.advanceTimersByTime(4001))
    expect(screen.getByRole('button', { name: 'Mở Én: Học đi bạn ê 👀' })).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Mở Én: Học đi bạn ê 👀' })).toHaveLength(1)
  })

  test('keeps the Tutor panel compact and responsive by contract', () => {
    vi.useFakeTimers()
    renderTutor()

    fireEvent.click(screen.getByRole('button', { name: 'Mở Én' }))
    act(() => vi.advanceTimersByTime(260))

    expect(screen.getByRole('dialog', { name: 'Én' })).toHaveClass('tutor-shell-compact-panel')
  })

  test('exposes premium interaction contracts for logo and functional buttons', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'LUMEN IELTS AI Tutor' })).toHaveClass('brand', 'brand-interactive')
    expect(screen.getByRole('button', { name: 'Cài đặt' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Làm bài Test đánh giá năng lực' })).toHaveClass('button', 'button-interactive')
    expect(screen.getByRole('button', { name: 'Tìm bài luyện' })).toHaveClass('button-interactive')
  })

  test('suppresses reminder bubbles when proactive AI is disabled but keeps manual Tutor opening', async () => {
    vi.useFakeTimers()
    vi.spyOn(Math, 'random').mockReturnValue(0)
    writeGuestPreferences({ proactiveAiEnabled: false })
    renderTutor()

    act(() => vi.advanceTimersByTime(180000))
    expect(screen.queryByRole('button', { name: /Mở Én: / })).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Mở Én' }))
    act(() => vi.advanceTimersByTime(260))
    expect(screen.getByRole('dialog', { name: 'Én' })).toBeInTheDocument()
  })

  test('suppresses reminder bubbles while Tutor is open', () => {
    vi.useFakeTimers()
    vi.spyOn(Math, 'random').mockReturnValue(0)
    renderTutor()

    fireEvent.click(screen.getByRole('button', { name: 'Mở Én' }))
    act(() => vi.advanceTimersByTime(260))
    act(() => vi.advanceTimersByTime(180000))

    expect(screen.queryByRole('button', { name: /Mở Én: / })).not.toBeInTheDocument()
  })
})

describe('premium UI polish', () => {
  test('uses Én for visible assistant identity in the hero visual', () => {
    render(<HeroVisual />)

    expect(screen.getByText('Én')).toBeInTheDocument()
    expect(screen.queryByText('IELTS AI Tutor')).not.toBeInTheDocument()
  })

  test('keeps hero content and marks the visual as enlarged without replacing its visual concept', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Bứt phá Band điểm IELTS cùng Én Độc quyền' })).toBeInTheDocument()
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
