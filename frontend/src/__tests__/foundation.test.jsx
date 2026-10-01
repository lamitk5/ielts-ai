import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test } from 'vitest'
import App from '../App'

function renderApp() {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <App />
    </MemoryRouter>,
  )
}

describe('frontend foundation', () => {
  test('renders the homepage shell with navigation and proof content', () => {
    renderApp()

    expect(screen.getByRole('main')).toBeInTheDocument()
    expect(
      within(screen.getByRole('navigation', { name: 'Primary navigation' })).getByRole(
        'link',
        { name: 'Trang chủ' },
      ),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('heading', {
        name: 'Bứt phá Band điểm IELTS cùng Én Độc quyền',
      }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Làm bài Test đánh giá năng lực' })).toBeEnabled()
    expect(
      within(screen.getByRole('region', {
        name: 'Bứt phá Band điểm IELTS cùng Én Độc quyền',
      })).getByText(/Reading, Listening, Writing và Speaking/),
    ).toBeInTheDocument()
    expect(screen.getByRole('contentinfo')).toBeInTheDocument()
  })

  test('exposes the mobile menu toggle as an accessible button', async () => {
    const { getByRole } = renderApp()
    const menuButton = getByRole('button', { name: 'Open navigation menu' })

    expect(menuButton).toHaveAttribute('aria-expanded', 'false')
  })
})
