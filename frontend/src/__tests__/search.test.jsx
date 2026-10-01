import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test } from 'vitest'
import App from '../App'

describe('practice search route', () => {
  test('renders query results with skill links and an empty-safe state', async () => {
    render(
      <MemoryRouter initialEntries={['/practice/search?q=IELTS%20Writing%20Task%201']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Tìm bài luyện tập' })).toBeInTheDocument()
    expect(await screen.findByText('Academic Writing Task 1')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Academic Writing Task 1/ })).toHaveAttribute('href', '/practice/writing')
  })
})
