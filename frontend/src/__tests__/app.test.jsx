import { render, screen, within } from '@testing-library/react'
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

    expect(screen.queryByText(/guest mode/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/6\.5/)).not.toBeInTheDocument()
    expect(screen.queryByTestId('member-progress')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Làm bài Test đánh giá năng lực' })).toBeEnabled()
  })

  test('assessment entry navigates to the assessment entry flow', async () => {
    const user = userEvent.setup()
    renderApp('/')

    await user.click(screen.getByRole('button', { name: 'Làm bài Test đánh giá năng lực' }))

    expect(screen.getByRole('heading', { name: 'Đánh giá năng lực IELTS' })).toBeInTheDocument()
  })

  test('member demo query keeps the home route usable without exposing mode labels', () => {
    renderApp('/?demo=member')

    expect(screen.queryByText(/member demo/i)).not.toBeInTheDocument()
    expect(
      screen.getByRole('heading', {
        name: 'Bứt phá Band điểm IELTS cùng Trợ giảng AI Độc quyền',
      }),
    ).toBeInTheDocument()
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

  test('provider composition exposes accessible Settings drawer for guest and authenticated learners', async () => {
    const user = userEvent.setup()
    renderApp('/')

    const settingsBtn = screen.getByRole('button', { name: /mở cài đặt|cài đặt/i })
    expect(settingsBtn).toBeInTheDocument()

    await user.click(settingsBtn)
    expect(screen.getByRole('dialog', { name: 'Cài đặt' })).toBeInTheDocument()
    expect(screen.getByText('Tùy chỉnh trải nghiệm học của bạn.')).toBeInTheDocument()
  })

  test('guest opening Tutor on writing practice sees personalized auth guidance with returnTo path', async () => {
    const user = userEvent.setup()
    renderApp('/practice/writing')

    const tutorBtn = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    await user.click(tutorBtn)

    const dialog = screen.getByRole('dialog', { name: 'Trợ giảng AI' })
    expect(dialog).toBeInTheDocument()
    expect(within(dialog).getByRole('link', { name: 'Đăng nhập' })).toBeInTheDocument()
    expect(screen.queryByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' })).not.toBeInTheDocument()
  })

  test('mobile navigation and settings drawer coordinate open states without conflicting', async () => {
    const user = userEvent.setup()
    renderApp('/')

    const menuToggle = screen.getByRole('button', { name: /open navigation menu/i })
    await user.click(menuToggle)
    expect(menuToggle).toHaveAttribute('aria-expanded', 'true')

    const settingsBtn = screen.getByRole('button', { name: /mở cài đặt|cài đặt/i })
    await user.click(settingsBtn)
    expect(screen.getByRole('dialog', { name: 'Cài đặt' })).toBeInTheDocument()
    expect(menuToggle).toHaveAttribute('aria-expanded', 'false')
  })
})
