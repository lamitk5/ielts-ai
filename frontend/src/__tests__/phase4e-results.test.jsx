import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test, vi } from 'vitest'
import LearnerResultShell from '../components/results/LearnerResultShell'
import SubmissionHistoryList from '../components/results/SubmissionHistoryList'
import AdminSubmissionReviewPage from '../pages/AdminSubmissionReviewPage'

describe('Phase 4E result surfaces', () => {
  test('shows trusted objective result and separate tutor/history actions', () => {
    render(<MemoryRouter><LearnerResultShell result={{ skill: 'READING', resultStatus: 'READY', score: 8, total: 10, accuracy: 80, actions: [] }} /></MemoryRouter>)
    expect(screen.getByText('8/10')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Hỏi Én về bài này' })).toHaveAttribute('href', '/tutor')
    expect(screen.getByRole('link', { name: 'Xem lịch sử bài làm' })).toHaveAttribute('href', '/practice/history')
  })

  test('uses a useful empty state without fabricating learner data', () => {
    render(<MemoryRouter><SubmissionHistoryList /></MemoryRouter>)
    expect(screen.getByText('Chưa có bài đã hoàn thành.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Khám phá bài luyện' })).toHaveAttribute('href', '/practice')
  })

  test('admin review page renders queue from the admin API', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ items: [{ id: 's1', skill: 'WRITING', status: 'PENDING_REVIEW' }] }) }))
    render(<MemoryRouter><AdminSubmissionReviewPage /></MemoryRouter>)
    expect(await screen.findByRole('button', { name: /WRITING/ })).toBeInTheDocument()
    vi.unstubAllGlobals()
  })
})
