import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import AdminLayout from '../components/admin/AdminLayout'
import AdminKnowledgePage from '../pages/AdminKnowledgePage'
import AdminReviewQueuePage from '../pages/AdminReviewQueuePage'
import { adminPortalApi } from '../services/adminPortalApi'

vi.mock('../services/adminPortalApi', () => ({
  adminPortalApi: {
    prompts: vi.fn(),
    reviews: vi.fn(),
    resolveReport: vi.fn(),
  },
}))

beforeEach(() => {
  vi.clearAllMocks()
  adminPortalApi.prompts.mockResolvedValue([])
  adminPortalApi.reviews.mockResolvedValue([])
})

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

  test('loads knowledge and review pages without treating async loaders as effect cleanup', async () => {
    const knowledge = render(<MemoryRouter><AdminKnowledgePage /></MemoryRouter>)
    expect(await screen.findByRole('heading', { name: 'Knowledge & Prompts' })).toBeInTheDocument()
    expect(() => knowledge.unmount()).not.toThrow()

    const review = render(<MemoryRouter><AdminReviewQueuePage /></MemoryRouter>)
    expect(await screen.findByRole('heading', { name: 'Review Queue' })).toBeInTheDocument()
    expect(() => review.unmount()).not.toThrow()
  })
})
