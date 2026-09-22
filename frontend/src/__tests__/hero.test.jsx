import { render, screen, within } from '@testing-library/react'
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
        name: 'Bứt phá Band điểm IELTS cùng Trợ giảng AI Độc quyền',
      }),
    ).toBeInTheDocument()
    expect(
      within(screen.getByRole('region', {
        name: 'Bứt phá Band điểm IELTS cùng Trợ giảng AI Độc quyền',
      })).getByText(/Reading, Listening, Writing và Speaking/),
    ).toBeInTheDocument()
    expect(screen.getByRole('search')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Tìm nội dung luyện tập IELTS' })).toHaveAttribute(
      'placeholder',
      'IELTS Writing Task 1 Line Graph',
    )
    expect(screen.getByRole('button', { name: 'Tìm bài luyện' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Làm bài Test đánh giá năng lực' })).toBeInTheDocument()
    expect(screen.getByTestId('hero-visual')).toHaveAttribute('aria-hidden', 'true')
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

  test('navigates a trimmed search query to the safe search route', async () => {
    const user = userEvent.setup()
    renderApp()
    const input = screen.getByRole('textbox', { name: 'Tìm nội dung luyện tập IELTS' })

    await user.type(input, '  IELTS Writing Task 1  ')
    await user.click(screen.getByRole('button', { name: 'Tìm bài luyện' }))

    expect(screen.getByRole('heading', { name: 'Tìm bài luyện tập' })).toBeInTheDocument()
    expect(screen.getByText(/IELTS Writing Task 1/)).toBeInTheDocument()
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
        name: 'Bứt phá Band điểm IELTS cùng Trợ giảng AI Độc quyền',
      }),
    ).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Tìm nội dung luyện tập IELTS' })).toBeInTheDocument()
  })
})
