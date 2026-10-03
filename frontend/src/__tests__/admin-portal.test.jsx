import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import AdminLayout from '../components/admin/AdminLayout'
import AdminKnowledgePage from '../pages/AdminKnowledgePage'
import AdminReviewQueuePage from '../pages/AdminReviewQueuePage'
import Navbar from '../components/layout/Navbar'
import { AuthProvider } from '../features/auth/AuthProvider'
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
  test('renders grouped administrator navigation in Vietnamese without learner-only links', () => {
    render(<MemoryRouter><AdminLayout /></MemoryRouter>)

    expect(screen.getByRole('navigation', { name: 'Điều hướng quản trị' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Tổng quan' })).toHaveAttribute('href', '/admin')
    expect(screen.getByRole('link', { name: 'Trình tạo bài AI' })).toHaveAttribute('href', '/admin/generator')
    expect(screen.getByRole('link', { name: 'Ngân hàng bài luyện' })).toHaveAttribute('href', '/admin/practices')
    expect(screen.getByRole('link', { name: 'Kho tri thức & Prompt' })).toHaveAttribute('href', '/admin/knowledge')
    expect(screen.getByRole('link', { name: 'Hàng đợi đánh giá' })).toHaveAttribute('href', '/admin/reviews')
    expect(screen.getByRole('link', { name: 'Học viên' })).toHaveAttribute('href', '/admin/learners')
    expect(screen.getByRole('link', { name: 'Sử dụng API & Chi phí' })).toHaveAttribute('href', '/admin/usage')
    expect(screen.getByRole('link', { name: 'Nhật ký hoạt động' })).toHaveAttribute('href', '/admin/audit')
    expect(screen.queryByRole('link', { name: 'Trang chủ' })).not.toBeInTheDocument()
  })

  test('shows the admin portal entry only for authenticated administrators', () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'admin', user: { id: 'admin-1', email: 'admin@gmail.com', role: 'ADMIN' } }))
    const { unmount } = render(<MemoryRouter><AuthProvider><Navbar /></AuthProvider></MemoryRouter>)
    expect(screen.getByRole('link', { name: 'Quản trị' })).toHaveAttribute('href', '/admin')

    unmount()
    localStorage.clear()
    render(<MemoryRouter><AuthProvider><Navbar /></AuthProvider></MemoryRouter>)
    expect(screen.queryByRole('link', { name: 'Quản trị' })).not.toBeInTheDocument()
  })

  test('loads knowledge and review pages without treating async loaders as effect cleanup', async () => {
    const knowledge = render(<MemoryRouter><AdminKnowledgePage /></MemoryRouter>)
    expect(await screen.findByRole('heading', { name: 'Quản lý Prompt' })).toBeInTheDocument()
    expect(() => knowledge.unmount()).not.toThrow()

    const review = render(<MemoryRouter><AdminReviewQueuePage /></MemoryRouter>)
    expect(await screen.findByRole('heading', { name: 'Hàng đợi đánh giá' })).toBeInTheDocument()
    expect(() => review.unmount()).not.toThrow()
  })
})
