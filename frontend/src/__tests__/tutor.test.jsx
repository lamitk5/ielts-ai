import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, test, vi } from 'vitest'
import App from '../App'
import FloatingTutor from '../components/tutor/FloatingTutor'
import { tutorGroundedDemo, tutorInsufficientDemo } from '../data/homepageMockData'

function renderApp(initialEntry = '/') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <App />
    </MemoryRouter>
  )
}

afterEach(() => {
  vi.useRealTimers()
  document.body.style.overflow = ''
})

describe('floating AI tutor', () => {
  test('opens, focuses the dialog, closes with Escape, and restores focus', async () => {
    const user = userEvent.setup()
    render(<FloatingTutor />)

    const trigger = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    await user.click(trigger)

    expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' })).toHaveFocus()

    await user.keyboard('{Escape}')

    expect(screen.queryByRole('dialog', { name: 'Trợ giảng AI' })).not.toBeInTheDocument()
    expect(trigger).toHaveFocus()
  })

  test('sends a local message, shows skeleton loading, and renders grounded sources', async () => {
    const user = userEvent.setup()
    render(<FloatingTutor />)

    await user.click(screen.getByRole('button', { name: 'Mở Trợ giảng AI' }))
    const input = screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' })
    await user.type(input, 'Giải thích lỗi Writing của tôi')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))

    expect(
      within(screen.getByRole('list', { name: 'Tin nhắn Trợ giảng AI' })).getByText(
        'Giải thích lỗi Writing của tôi',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveAttribute('aria-busy', 'true')

    await waitFor(() => expect(screen.getByText(tutorGroundedDemo.content)).toBeInTheDocument())
    expect(
      within(screen.getByRole('list', { name: 'Tin nhắn Trợ giảng AI' })).getByText(
        (_, element) =>
          element?.classList.contains('tutor-grounding-badge') &&
          element.textContent.includes('Dựa trên nguồn tham chiếu đã kiểm chứng'),
      ),
    ).toBeInTheDocument()
    expect(screen.getByText('Rubric Writing Task 2')).toBeInTheDocument()
  })

  test('renders the insufficient-context state without invented citations', async () => {
    const user = userEvent.setup()
    render(<FloatingTutor />)

    await user.click(screen.getByRole('button', { name: 'Mở Trợ giảng AI' }))
    await user.type(
      screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' }),
      'Tôi cần thêm ngữ cảnh',
    )
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))

    await waitFor(() => expect(screen.getByText(tutorInsufficientDemo.content)).toBeInTheDocument())
    expect(screen.getByText('Cần thêm ngữ cảnh')).toBeInTheDocument()
    expect(screen.queryByText('Rubric Writing Task 2')).not.toBeInTheDocument()
    expect(screen.queryByText(/certified examiner|Cambridge IELTS|100% accuracy/i)).not.toBeInTheDocument()
  })

  test('keeps the tutor preview and final CTA in the homepage flow', () => {
    renderApp()

    expect(screen.getByRole('region', { name: 'Trợ giảng AI' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Sẵn sàng bắt đầu lộ trình IELTS của bạn?' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Bắt đầu đánh giá năng lực' })).toHaveAttribute(
      'href',
      '/assessment',
    )
    expect(screen.getByRole('link', { name: 'Luyện tập 4 kỹ năng' })).toHaveAttribute(
      'href',
      '/#skills',
    )
  })
})
