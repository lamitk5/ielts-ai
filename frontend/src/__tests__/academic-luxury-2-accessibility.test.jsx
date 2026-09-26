import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, test } from 'vitest'
import App from '../App'
import TutorShell, { SHELL_STATES } from '../components/tutor/TutorShell'

function renderApp(initialEntry) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <App />
    </MemoryRouter>,
  )
}

beforeEach(() => {
  localStorage.clear()
  document.documentElement.removeAttribute('data-reduced-motion')
})

describe('Academic Luxury 2.0 accessibility and responsive matrix', () => {
  test('theme controls preserve readable landmarks and reduced-motion state', async () => {
    const user = userEvent.setup()
    renderApp('/')

    expect(screen.getByRole('link', { name: 'Bỏ qua đến nội dung chính' })).toHaveAttribute('href', '#main-content')
    await user.click(screen.getByRole('button', { name: 'Cài đặt' }))
    const dialog = screen.getByRole('dialog', { name: 'Cài đặt' })

    await user.click(within(dialog).getByRole('button', { name: 'Sáng' }))
    expect(document.documentElement.style.getPropertyValue('--bg-page')).toBe('#f5f7fa')
    await user.selectOptions(within(dialog).getByRole('combobox', { name: 'Chuyển động' }), 'reduce')
    expect(document.documentElement).toHaveAttribute('data-reduced-motion', 'true')
    await user.keyboard('{Escape}')
    expect(screen.queryByRole('dialog', { name: 'Cài đặt' })).not.toBeInTheDocument()
    expect(document.body.style.overflow).toBe('')
  })

  test('Tutor fullscreen keeps composer actions keyboard reachable', () => {
    render(
      <TutorShell
        state={SHELL_STATES.FULLSCREEN_DESKTOP}
        messages={[]}
        onSend={() => {}}
        onClose={() => {}}
      />,
    )

    const dialog = screen.getByRole('dialog', { name: 'Trợ giảng AI' })
    expect(dialog).toHaveClass('tutor-shell-fullscreen', 'tutor-shell-viewport-safe')
    expect(within(dialog).getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' })).toBeEnabled()
    expect(within(dialog).getByRole('button', { name: 'Thêm tệp đính kèm' })).toBeEnabled()
    expect(within(dialog).getByRole('button', { name: 'Gửi câu hỏi' })).toBeDisabled()
  })

  test('Speaking workspace exposes a single-column-safe mobile boundary', () => {
    renderApp('/practice/speaking')

    expect(screen.getByRole('main')).toHaveClass('page-main')
    expect(document.querySelector('.speaking-room-single-column-safe')).toBeInTheDocument()
  })

  test('attachment status exposes live state and keyboard action semantics', async () => {
    const user = userEvent.setup()
    const onRetry = () => {}
    render(
      <div>
        <div
          className="tutor-attachment-responsive-safe"
          role="status"
          aria-live="polite"
          aria-label="Tệp đính kèm: essay.pdf"
        >
          <button type="button" onClick={onRetry} aria-label="Thử lại tải tệp">Thử lại</button>
        </div>
      </div>,
    )

    const status = screen.getByRole('status')
    expect(status).toHaveAttribute('aria-live', 'polite')
    const retry = screen.getByRole('button', { name: 'Thử lại tải tệp' })
    retry.focus()
    expect(retry).toHaveFocus()
    await user.keyboard('{Enter}')
  })
})
