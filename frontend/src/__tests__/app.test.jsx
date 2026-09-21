import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test } from 'vitest'
import App from '../App'
import { memberDemo } from '../data/homepageMockData'

function renderApp(initialEntry = '/') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <App />
    </MemoryRouter>,
  )
}

describe('app shell and routing', () => {
  test('renders the home route with Navbar, main content, and Footer', () => {
    renderApp('/')

    expect(screen.getByRole('navigation', { name: 'Primary navigation' })).toBeInTheDocument()
    expect(screen.getByRole('main')).toBeInTheDocument()
    expect(screen.getByRole('contentinfo')).toBeInTheDocument()
  })

  test('defaults to guest mode without personal bands and exposes assessment entry', () => {
    renderApp('/')

    expect(screen.getByText('Guest mode')).toBeInTheDocument()
    expect(screen.queryByText(/6\.5/)).not.toBeInTheDocument()
    expect(screen.queryByTestId('member-progress')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Bắt đầu đánh giá' })).toBeEnabled()
  })

  test('assessment entry navigates to the safe assessment placeholder', async () => {
    const user = userEvent.setup()
    renderApp('/')

    await user.click(screen.getByRole('button', { name: 'Bắt đầu đánh giá' }))

    expect(screen.getByRole('heading', { name: /Assessment/ })).toBeInTheDocument()
  })

  test('member demo query selects the centralized member state', () => {
    renderApp('/?demo=member')

    expect(screen.getByText('Member demo · Đăng')).toBeInTheDocument()
    expect(memberDemo.user.firstName).toBe('Đăng')
    expect(memberDemo.progress.map(({ skill }) => skill)).toEqual([
      'Reading',
      'Listening',
      'Writing',
      'Speaking',
    ])
  })

  test.each([
    '/',
    '/assessment',
    '/practice/reading',
    '/practice/listening',
    '/practice/writing',
    '/practice/speaking',
    '/login',
  ])('renders a safe placeholder-capable shell for %s', (route) => {
    renderApp(route)

    expect(screen.getByRole('main')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument()
    expect(screen.getByRole('contentinfo')).toBeInTheDocument()
  })

  test('unknown practice skills render a safe fallback instead of crashing', () => {
    renderApp('/practice/unknown')

    expect(screen.getByRole('heading', { name: /Practice area unavailable/ })).toBeInTheDocument()
    expect(screen.getByText(/unknown/)).toBeInTheDocument()
  })
})
