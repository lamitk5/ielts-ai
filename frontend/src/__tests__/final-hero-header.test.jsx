import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test } from 'vitest'
import App from '../App'

function renderApp(initialEntry = '/') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <App />
    </MemoryRouter>,
  )
}

describe('final hero and LUMEN header polish', () => {
  test('renders a full-bleed bookshelf hero with an explicit light/dark theme state', () => {
    renderApp()

    const hero = screen.getByRole('region', { name: 'Bứt phá Band điểm IELTS cùng Trợ giảng AI Độc quyền' })
    expect(hero).toHaveClass('hero-section-full-bleed')
    expect(screen.getByTestId('hero-bookshelf-background')).toHaveAttribute('data-full-bleed', 'true')
    expect(document.documentElement).toHaveAttribute('data-theme', 'light')
  })

  test('renders the approved LUMEN brand lockup and refined utility controls', () => {
    renderApp()

    const navigation = screen.getByRole('navigation', { name: 'Primary navigation' })
    expect(within(navigation).getByRole('link', { name: 'LUMEN IELTS AI Tutor' })).toHaveClass('brand')
    expect(within(navigation).getByTestId('lumen-logo-mark')).toBeInTheDocument()
    expect(within(navigation).getByRole('button', { name: 'Cài đặt' })).toHaveClass('settings-trigger-utility')
  })

  test('keeps active navigation and keyboard mobile menu semantics', async () => {
    const user = userEvent.setup()
    renderApp()

    const navigation = screen.getByRole('navigation', { name: 'Primary navigation' })
    expect(within(navigation).getByRole('link', { name: 'Trang chủ' })).toHaveAttribute('aria-current', 'page')
    const menuToggle = screen.getByRole('button', { name: 'Open navigation menu' })
    await user.click(menuToggle)
    expect(menuToggle).toHaveAttribute('aria-expanded', 'true')
    expect(navigation).toHaveAttribute('data-menu-open', 'true')
  })
})
