import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, test } from 'vitest'
import App from '../App'

function renderApp(initialEntry) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <App />
    </MemoryRouter>,
  )
}

beforeEach(() => {
  localStorage.clear()
})

describe('Academic Luxury 2.0 route and product regression matrix', () => {
  test.each([
    ['/','Bứt phá Band điểm IELTS cùng Trợ giảng AI Độc quyền'],
    ['/assessment', 'Đánh giá năng lực IELTS'],
    ['/practice/reading', 'Reading'],
    ['/practice/listening', 'Listening'],
    ['/practice/writing', 'Writing'],
    ['/practice/speaking', 'Speaking'],
    ['/login', 'Đăng nhập'],
    ['/practice/unknown', 'Practice area unavailable'],
  ])('keeps the primary route available: %s', (route, heading) => {
    renderApp(route)

    expect(screen.getByRole('main')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: new RegExp(heading, 'i') })).toBeInTheDocument()
    expect(screen.getByRole('contentinfo')).toBeInTheDocument()
  })

  test('keeps the guest homepage truthful and exposes the assessment path', () => {
    renderApp('/')

    expect(screen.queryByText(/guest mode|member demo/i)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Làm bài Test đánh giá năng lực' })).toBeEnabled()
    expect(screen.getByRole('link', { name: 'Khám phá 4 kỹ năng' })).toHaveAttribute('href', '/#skills')
    expect(screen.queryByTestId('member-progress')).not.toBeInTheDocument()
  })

  test('keeps member demo analytics limited to the four named skills', () => {
    renderApp('/?demo=member')

    const progress = screen.getByRole('region', { name: 'Tiến độ luyện tập của bạn' })
    for (const skill of ['Reading', 'Listening', 'Writing', 'Speaking']) {
      expect(within(progress).getByText(skill)).toBeInTheDocument()
    }
    expect(screen.queryByText(/guest mode|member demo/i)).not.toBeInTheDocument()
  })

  test('keeps search query state on the safe practice search route', () => {
    renderApp('/practice/search?q=IELTS%20Writing%20Task%201')

    expect(screen.getByRole('heading', { name: 'Tìm bài luyện tập' })).toBeInTheDocument()
    expect(screen.getByText(/IELTS Writing Task 1/)).toBeInTheDocument()
  })

  test('does not surface future-phase claims in the product shell', () => {
    renderApp('/')

    expect(screen.queryByText(/official IELTS|chấm điểm chính thức|real-time STT|vision analysis/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/PostgreSQL|pgvector|Gemini|Groq|Cloudflare/i)).not.toBeInTheDocument()
  })
})
