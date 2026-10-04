import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test } from 'vitest'
import App from '../App'

describe('platform entry routes', () => {
  test('renders a useful assessment entry instead of a placeholder', () => {
    render(<MemoryRouter initialEntries={['/assessment']}><App /></MemoryRouter>)

    expect(screen.getByRole('heading', { name: 'Đánh giá năng lực IELTS' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Bắt đầu với Reading' })).toHaveAttribute('href', '/practice/reading')
  })

  test('renders the registration form linked from login', () => {
    render(<MemoryRouter initialEntries={['/register']}><App /></MemoryRouter>)

    expect(screen.getByRole('heading', { name: 'Tạo tài khoản' })).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toBeInTheDocument()
    expect(screen.getByLabelText('Mật khẩu')).toBeInTheDocument()
  })
})
