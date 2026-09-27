import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test, vi } from 'vitest'
import App from '../App'
import FloatingTutor from '../components/tutor/FloatingTutor'

function renderTutor() {
  return render(
    <MemoryRouter>
      <FloatingTutor />
    </MemoryRouter>,
  )
}

describe('AI Tutor mascot launcher', () => {
  test('replaces the legacy launcher with an accessible mascot button and tooltip', () => {
    renderTutor()

    const launcher = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    expect(launcher).toHaveClass('ai-tutor-mascot-launcher')
    expect(launcher).toHaveAttribute('data-pointer-direction', 'center')
    expect(screen.getByText('Trợ giảng AI')).toHaveClass('ai-tutor-mascot-tooltip')
    expect(screen.queryByTestId('floating-tutor-button')).not.toBeInTheDocument()
  })

  test('opens the existing Tutor on click, Enter, and Space without a duplicate launcher', async () => {
    const user = userEvent.setup()
    renderTutor()

    const launcher = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    await user.click(launcher)
    expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Mở Trợ giảng AI' })).not.toBeInTheDocument()

    await user.click(within(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).getByRole('button', { name: 'Đóng Trợ giảng AI' }))
    const restoredLauncher = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    restoredLauncher.focus()
    await user.keyboard('{Enter}')
    expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toBeInTheDocument()

    await user.click(within(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).getByRole('button', { name: 'Đóng Trợ giảng AI' }))
    screen.getByRole('button', { name: 'Mở Trợ giảng AI' }).focus()
    await user.keyboard(' ')
    expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toBeInTheDocument()
  })

  test('tracks a pointer direction for fine pointers and remains keyboard-safe', async () => {
    renderTutor()
    const launcher = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    launcher.getBoundingClientRect = () => ({ left: 100, top: 100, width: 80, height: 80 })

    fireEvent.pointerMove(launcher, { pointerType: 'mouse', clientX: 175, clientY: 140 })
    await waitFor(() => expect(launcher).toHaveAttribute('data-pointer-direction', 'right'))
    fireEvent.pointerLeave(launcher, { pointerType: 'mouse' })
    expect(launcher).toHaveAttribute('data-pointer-direction', 'center')
    expect(launcher).toHaveClass('ai-tutor-mascot-launcher')
  })

  test('keeps the mascot usable with reduced motion and ignores touch tracking', async () => {
    const originalMatchMedia = window.matchMedia
    window.matchMedia = vi.fn().mockImplementation((query) => ({
      matches: query === '(prefers-reduced-motion: reduce)',
      media: query,
      addEventListener: () => {},
      removeEventListener: () => {},
      addListener: () => {},
      removeListener: () => {},
    }))

    try {
      const user = userEvent.setup()
      renderTutor()
      const launcher = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
      fireEvent.pointerMove(launcher, { pointerType: 'touch', clientX: 999, clientY: 999 })
      expect(launcher).toHaveAttribute('data-pointer-direction', 'center')
      await user.click(launcher)
      expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toBeInTheDocument()
    } finally {
      window.matchMedia = originalMatchMedia
    }
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
