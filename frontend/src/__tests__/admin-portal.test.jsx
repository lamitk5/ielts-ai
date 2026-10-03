import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test } from 'vitest'
import AdminLayout from '../components/admin/AdminLayout'

describe('admin portal shell', () => {
  test('renders grouped administrator navigation without learner-only links', () => {
    render(<MemoryRouter><AdminLayout /></MemoryRouter>)

    expect(screen.getByRole('navigation', { name: 'Điều hướng quản trị' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Dashboard' })).toHaveAttribute('href', '/admin')
    expect(screen.getByRole('link', { name: 'AI Generator' })).toHaveAttribute('href', '/admin/generator')
    expect(screen.getByRole('link', { name: 'Practice Bank' })).toHaveAttribute('href', '/admin/practices')
    expect(screen.getByRole('link', { name: 'Knowledge & Prompts' })).toHaveAttribute('href', '/admin/knowledge')
    expect(screen.getByRole('link', { name: 'Review Queue' })).toHaveAttribute('href', '/admin/reviews')
    expect(screen.getByRole('link', { name: 'Learners' })).toHaveAttribute('href', '/admin/learners')
    expect(screen.getByRole('link', { name: 'API Usage & Cost' })).toHaveAttribute('href', '/admin/usage')
    expect(screen.getByRole('link', { name: 'Audit Log' })).toHaveAttribute('href', '/admin/audit')
    expect(screen.queryByRole('link', { name: 'Trang chủ' })).not.toBeInTheDocument()
  })
})
