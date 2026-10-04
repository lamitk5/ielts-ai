import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import MockTestPage from '../pages/MockTestPage'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import * as mockTestApi from '../services/mockTestApi'

vi.mock('../services/mockTestApi')

describe('Mock Test Flow and Recovery Acceptance', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const mockSession = {
    id: 'mock-session-001',
    mockTestId: 'mock-full-academic-01',
    status: 'IN_PROGRESS',
    totalTimeLimitSeconds: 10200,
    elapsedSeconds: 120,
    currentSectionIndex: 0,
    sections: [
      { id: 'sec-0', sectionOrder: 0, skill: 'LISTENING', timeLimitSeconds: 1800, status: 'IN_PROGRESS' },
      { id: 'sec-1', sectionOrder: 1, skill: 'READING', timeLimitSeconds: 3600, status: 'NOT_STARTED' },
      { id: 'sec-2', sectionOrder: 2, skill: 'WRITING', timeLimitSeconds: 3600, status: 'NOT_STARTED' },
      { id: 'sec-3', sectionOrder: 3, skill: 'SPEAKING', timeLimitSeconds: 1200, status: 'NOT_STARTED' },
    ],
  }

  it('completes flow from start -> autosave -> next section -> submit -> report view', async () => {
    mockTestApi.startOrResumeMockSession.mockResolvedValue(mockSession)
    mockTestApi.autosaveMockSectionDraft.mockResolvedValue({ revision: 1 })
    mockTestApi.executeMockCommand.mockImplementation(async (id, cmd) => {
      if (cmd === 'PAUSE') return { ...mockSession, status: 'PAUSED' }
      if (cmd === 'RESUME') return { ...mockSession, status: 'IN_PROGRESS' }
      if (cmd === 'SUBMIT') return { ...mockSession, status: 'COMPLETED' }
      return mockSession
    })
    mockTestApi.getMockTestResult.mockResolvedValue({
      sessionId: 'mock-session-001',
      mockTestId: 'mock-full-academic-01',
      resultStatus: 'READY',
      estimatedOverallBand: 7.0,
      estimatedBandLabel: 'Band ước lượng tổng thể',
      aiDisclaimer: 'Band ước lượng bởi AI — Không phải điểm thi IELTS chính thức.',
      totalDurationSeconds: 10200,
      sectionResults: [
        { sectionOrder: 0, skill: 'LISTENING', practiceId: 'p-lis', submissionId: 'sub-0', sectionStatus: 'COMPLETED', learnerResult: { score: 30, total: 40, estimatedBand: 7.0 } },
        { sectionOrder: 1, skill: 'READING', practiceId: 'p-read', submissionId: 'sub-1', sectionStatus: 'COMPLETED', learnerResult: { score: 32, total: 40, estimatedBand: 7.0 } },
        { sectionOrder: 2, skill: 'WRITING', practiceId: 'p-wri', submissionId: 'sub-2', sectionStatus: 'COMPLETED', learnerResult: { estimatedBand: 7.0 } },
        { sectionOrder: 3, skill: 'SPEAKING', practiceId: 'p-spk', submissionId: 'sub-3', sectionStatus: 'COMPLETED', learnerResult: { humanReview: { overallBand: 7.0, status: 'COMPLETED' } } },
      ],
    })

    render(
      <MemoryRouter initialEntries={['/practice/mock-test']}>
        <Routes>
          <Route path="/practice/mock-test" element={<MockTestPage />} />
        </Routes>
      </MemoryRouter>
    )

    // 1. Check Listening section loaded
    await waitFor(() => {
      expect(screen.getByText('IELTS Listening Section')).toBeInTheDocument()
    })

    // 2. Pause and Resume test
    fireEvent.click(screen.getByRole('button', { name: /Tạm dừng/i }))
    await waitFor(() => {
      expect(screen.getByText('Bài thi thử đang tạm dừng')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: /Tiếp tục làm bài/i }))
    await waitFor(() => {
      expect(screen.queryByText('Bài thi thử đang tạm dừng')).not.toBeInTheDocument()
    })

    // 3. Move to next section
    fireEvent.click(screen.getByRole('button', { name: /Hoàn thành phần này & Chuyển tiếp/i }))
    await waitFor(() => {
      expect(screen.getByText('IELTS Reading Section')).toBeInTheDocument()
    })

    // 4. Jump to final section (Speaking)
    fireEvent.click(screen.getByText('Speaking (Nói)'))
    await waitFor(() => {
      expect(screen.getByText('IELTS Speaking Section')).toBeInTheDocument()
    })

    // 5. Submit Mock Test
    const submitBtn = screen.getByRole('button', { name: /Hoàn tất & Nộp toàn bộ Mock Test/i })
    fireEvent.click(submitBtn)

    // 6. Result report displayed
    await waitFor(() => {
      expect(screen.getByText('Báo Cáo Tổng Hợp Mock Test')).toBeInTheDocument()
      expect(screen.getAllByText('7.0').length).toBeGreaterThan(0)
    })
  })
})
