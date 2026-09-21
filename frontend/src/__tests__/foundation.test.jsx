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
        { name: 'Home' },
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'IELTS AI' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Primary action' })).toBeEnabled()
    expect(screen.getByText('A calm foundation for focused IELTS practice.')).toBeInTheDocument()
  })

  test('exposes the mobile menu toggle as an accessible button', async () => {
    const { getByRole } = renderApp()
    const menuButton = getByRole('button', { name: 'Open navigation menu' })

    expect(menuButton).toHaveAttribute('aria-expanded', 'false')
  })
})
