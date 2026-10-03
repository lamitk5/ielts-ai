import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test, vi } from 'vitest'
import App from '../App'

function renderApp(initialEntry = '/') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <App />
    </MemoryRouter>,
  )
}

describe('premium hero', () => {
  test('renders the four-skill hero identity and semantic search controls', () => {
    renderApp()

    expect(screen.getByText('IELTS 4 KỸ NĂNG • AI TUTOR 24/7')).toBeInTheDocument()
    expect(screen.queryByText(/Academic Luxury/)).not.toBeInTheDocument()
    expect(screen.queryByText(/guest mode/i)).not.toBeInTheDocument()
    expect(
      screen.getByRole('heading', {
        name: 'Bứt phá Band điểm IELTS cùng Én Độc quyền',
      }),
    ).toBeInTheDocument()
    expect(
      within(screen.getByRole('region', {
        name: 'Bứt phá Band điểm IELTS cùng Én Độc quyền',
      })).getByText(/Reading, Listening, Writing và Speaking/),
    ).toBeInTheDocument()
    expect(screen.getByRole('search')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Tìm nội dung luyện tập IELTS' })).toHaveAttribute(
      'placeholder',
      'IELTS Writing Task 1 Line Graph',
    )
    expect(screen.getByRole('button', { name: 'Tìm bài luyện' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Làm bài Test đánh giá năng lực' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Khám phá 4 kỹ năng' })).toHaveAttribute('href', '/#skills')
    expect(screen.getByTestId('hero-visual')).toHaveAttribute('aria-hidden', 'true')
  })

  test('uses the compact editorial hero layout contract', () => {
    renderApp()

    const hero = screen.getByRole('region', { name: 'Bứt phá Band điểm IELTS cùng Én Độc quyền' })
    expect(hero).toHaveClass('hero-section', 'hero-editorial')
    expect(screen.getByRole('search')).toHaveClass('hero-search')
    expect(screen.getByRole('heading', { level: 1 })).toHaveClass('hero-title')
  })

  test('places assessment actions before quick suggestions', () => {
    renderApp()

    const primaryAction = screen.getByRole('button', { name: 'Làm bài Test đánh giá năng lực' })
    const firstSuggestion = screen.getByRole('button', { name: 'Reading' })

    expect(primaryAction.compareDocumentPosition(firstSuggestion)).toBe(Node.DOCUMENT_POSITION_FOLLOWING)
  })

  test('navigates the assessment CTA to the assessment route', async () => {
    const user = userEvent.setup()
    renderApp()

    await user.click(screen.getByRole('button', { name: 'Làm bài Test đánh giá năng lực' }))

    expect(
      screen.getByRole('heading', { name: 'Đánh giá năng lực IELTS' }),
    ).toBeInTheDocument()
  })

  test('keeps blank search on the page and announces validation', async () => {
    const user = userEvent.setup()
    renderApp()

    await user.click(screen.getByRole('button', { name: 'Tìm bài luyện' }))

    expect(screen.getByRole('search')).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Nhập nội dung bạn muốn luyện tập.',
    )
  })

  test('activates the liquid gold focus state without changing the search contract', async () => {
    const user = userEvent.setup()
    renderApp()

    const input = screen.getByRole('textbox', { name: 'Tìm nội dung luyện tập IELTS' })
    const control = input.closest('.hero-search-control')

    await user.click(input)

    expect(control).toHaveClass('hero-search-control-focused')
    expect(control).toHaveAttribute('data-search-focused', 'true')
  })

  test('follows a fine pointer with CSS coordinates without interfering with the input', () => {
    renderApp()

    const input = screen.getByRole('textbox', { name: 'Tìm nội dung luyện tập IELTS' })
    const control = input.closest('.hero-search-control')
    vi.spyOn(control, 'getBoundingClientRect').mockReturnValue({
      bottom: 56,
      height: 56,
      left: 0,
      right: 240,
      top: 0,
      width: 240,
      x: 0,
      y: 0,
      toJSON: () => ({}),
    })

    fireEvent.pointerMove(control, {
      clientX: 80,
      clientY: 24,
      pointerType: 'mouse',
    })

    expect(control).toHaveAttribute('data-pointer-active', 'true')
    expect(control.style.getPropertyValue('--search-pointer-x')).toBe('80px')
    expect(control.style.getPropertyValue('--search-pointer-y')).toBe('24px')
    expect(input).not.toHaveFocus()
  })

  test('keeps touch interaction independent from pointer-follow styling', () => {
    renderApp()

    const input = screen.getByRole('textbox', { name: 'Tìm nội dung luyện tập IELTS' })
    const control = input.closest('.hero-search-control')

    fireEvent.pointerMove(control, {
      clientX: 80,
      clientY: 24,
      pointerType: 'touch',
    })

    expect(control).not.toHaveAttribute('data-pointer-active', 'true')
    expect(control.style.getPropertyValue('--search-pointer-x')).toBe('50%')
    expect(control.style.getPropertyValue('--search-pointer-y')).toBe('50%')
  })

  test('navigates a trimmed search query to the safe search route', async () => {
    const user = userEvent.setup()
    renderApp()
    const input = screen.getByRole('textbox', { name: 'Tìm nội dung luyện tập IELTS' })

    await user.type(input, '  IELTS Writing Task 1  ')
    await user.click(screen.getByRole('button', { name: 'Tìm bài luyện' }))

    await waitFor(
      () => {
        expect(screen.getByRole('heading', { name: 'Tìm bài luyện tập' })).toBeInTheDocument()
      },
      { timeout: 3000 },
    )
    expect(screen.getByText(/IELTS Writing Task 1/)).toBeInTheDocument()
  })

  test('keeps the liquid sweep visible briefly before routing a submitted query', () => {
    vi.useFakeTimers()
    try {
      renderApp()
      const input = screen.getByRole('textbox', { name: 'Tìm nội dung luyện tập IELTS' })
      const control = input.closest('.hero-search-control')

      fireEvent.change(input, { target: { value: 'Reading' } })
      fireEvent.submit(screen.getByRole('search'))

      expect(control).toHaveClass('hero-search-control-submitting')
      expect(screen.getByRole('search')).toBeInTheDocument()

      act(() => {
        vi.advanceTimersByTime(450)
      })

      expect(screen.getByRole('heading', { name: 'Tìm bài luyện tập' })).toBeInTheDocument()
    } finally {
      vi.useRealTimers()
    }
  })

  test('exposes keyboard-accessible quick practice suggestions', async () => {
    const user = userEvent.setup()
    renderApp()

    const suggestions = [
      'Reading',
      'Listening',
      'Writing Task 2',
      'Speaking Part 2',
    ]

    for (const suggestion of suggestions) {
      const chip = screen.getByRole('button', { name: suggestion })
      expect(chip).toHaveAttribute('type', 'button')
      await user.tab()
    }

    expect(screen.getByRole('button', { name: 'Reading' })).toBeInTheDocument()
  })

  test('keeps the hero content visible when reduced motion is enabled', () => {
    window.matchMedia = vi.fn().mockImplementation((query) => ({
      matches: query === '(prefers-reduced-motion: reduce)',
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    }))

    renderApp()

    expect(
      screen.getByRole('heading', {
        name: 'Bứt phá Band điểm IELTS cùng Én Độc quyền',
      }),
    ).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Tìm nội dung luyện tập IELTS' })).toBeInTheDocument()
  })
})
