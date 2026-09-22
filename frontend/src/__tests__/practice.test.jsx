import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test } from 'vitest'
import App from '../App'

describe('deterministic practice routes', () => {
  test.each([
    ['/practice/reading', 'Reading practice'],
    ['/practice/listening', 'Listening practice'],
  ])('renders a usable %s shell', (route, heading) => {
    render(
      <MemoryRouter initialEntries={[route]}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: heading })).toBeInTheDocument()
    expect(screen.getByText(/synthetic practice set/i)).toBeInTheDocument()
  })

  test('unknown skill keeps a safe fallback', () => {
    render(
      <MemoryRouter initialEntries={['/practice/unknown']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: /Practice area unavailable/ })).toBeInTheDocument()
  })
})
