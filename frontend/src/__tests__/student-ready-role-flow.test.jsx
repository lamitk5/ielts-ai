import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, test } from 'vitest'
import App from '../App'

afterEach(() => localStorage.clear())

describe('student-ready role boundaries', () => {
  test('learner is redirected away from the admin generator route', () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'learner-token', user: { id: 'learner-1', role: 'CUSTOMER', email: 'learner@example.com' } }))

    render(<MemoryRouter initialEntries={['/admin/practice-generator']}><App /></MemoryRouter>)

    expect(screen.queryByRole('heading', { name: 'Trung tâm Tạo & Kiểm duyệt Đề thi AI' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /Bứt phá Band điểm IELTS/i })).toBeInTheDocument()
  })
})
