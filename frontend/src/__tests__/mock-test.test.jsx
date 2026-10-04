import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import MockTestTimer from '../components/mock/MockTestTimer'
import MockSectionNavigator from '../components/mock/MockSectionNavigator'
import MockTestShell from '../components/mock/MockTestShell'
import MockTestPage from '../pages/MockTestPage'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import * as mockTestApi from '../services/mockTestApi'

vi.mock('../services/mockTestApi')

describe('Mock Test Workspace Components', () => {
  describe('MockTestTimer', () => {
    it('renders remaining time correctly and handles countdown', () => {
      render(<MockTestTimer totalTimeLimitSeconds={3600} initialElapsedSeconds={60} isPaused={false} />)
      expect(screen.getByRole('timer')).toBeInTheDocument()
      expect(screen.getByText('59:00')).toBeInTheDocument()
    })

    it('displays paused badge when isPaused is true', () => {
      render(<MockTestTimer totalTimeLimitSeconds={3600} initialElapsedSeconds={60} isPaused={true} />)
      expect(screen.getByText('Tạm dừng')).toBeInTheDocument()
    })
  })

  describe('MockSectionNavigator', () => {
    const sections = [
      { id: 'sec-1', skill: 'LISTENING', status: 'COMPLETED' },
      { id: 'sec-2', skill: 'READING', status: 'IN_PROGRESS' },
      { id: 'sec-3', skill: 'WRITING', status: 'NOT_STARTED' },
      { id: 'sec-4', skill: 'SPEAKING', status: 'NOT_STARTED' },
    ]

    it('renders all 4 sections with current section highlight', () => {
      const onSelect = vi.fn()
      render(
        <MockSectionNavigator
          sections={sections}
          currentSectionIndex={1}
          onSelectSection={onSelect}
        />
      )

      expect(screen.getByText('Listening (Nghe)')).toBeInTheDocument()
      expect(screen.getByText('Reading (Đọc)')).toBeInTheDocument()
      expect(screen.getByText('Writing (Viết)')).toBeInTheDocument()
      expect(screen.getByText('Speaking (Nói)')).toBeInTheDocument()

      fireEvent.click(screen.getByText('Writing (Viết)'))
      expect(onSelect).toHaveBeenCalledWith(2)
    })
  })

  describe('MockTestShell', () => {
    const session = {
      id: 'sess-12345678',
      status: 'IN_PROGRESS',
      totalTimeLimitSeconds: 10200,
      elapsedSeconds: 300,
      sections: [
        { id: 's1', skill: 'LISTENING', status: 'IN_PROGRESS' },
        { id: 's2', skill: 'READING', status: 'NOT_STARTED' },
      ],
    }

    it('renders shell header, disclaimer badge, and child content', () => {
      render(
        <MockTestShell session={session} currentSectionIndex={0}>
          <div>Workspace Content Here</div>
        </MockTestShell>
      )

      expect(screen.getByText('IELTS Academic Mock Test')).toBeInTheDocument()
      expect(screen.getByText('Thi thử mô phỏng AI')).toBeInTheDocument()
      expect(screen.getByText('Workspace Content Here')).toBeInTheDocument()
    })

    it('shows pause modal when session status is PAUSED', () => {
      const pausedSession = { ...session, status: 'PAUSED' }
      render(
        <MockTestShell session={pausedSession} currentSectionIndex={0}>
          <div>Content</div>
        </MockTestShell>
      )

      expect(screen.getByText('Bài thi thử đang tạm dừng')).toBeInTheDocument()
    })
  })

  describe('MockTestPage Integration', () => {
    beforeEach(() => {
      vi.clearAllMocks()
    })

    it('loads session on mount and renders active section', async () => {
      mockTestApi.startOrResumeMockSession.mockResolvedValue({
        id: 'sess-test-1',
        mockTestId: 'mock-academic-01',
        status: 'IN_PROGRESS',
        totalTimeLimitSeconds: 7200,
        elapsedSeconds: 0,
        currentSectionIndex: 0,
        sections: [
          { id: 'sec-1', skill: 'LISTENING', timeLimitSeconds: 1800, status: 'IN_PROGRESS' },
          { id: 'sec-2', skill: 'READING', timeLimitSeconds: 3600, status: 'NOT_STARTED' },
        ],
      })
      mockTestApi.autosaveMockSectionDraft.mockResolvedValue({ revision: 1 })

      render(
        <MemoryRouter initialEntries={['/practice/mock-test']}>
          <Routes>
            <Route path="/practice/mock-test" element={<MockTestPage />} />
          </Routes>
        </MemoryRouter>
      )

      await waitFor(() => {
        expect(screen.getByText('IELTS Listening Section')).toBeInTheDocument()
      })

      expect(screen.getAllByPlaceholderText('Nhập câu trả lời...').length).toBeGreaterThan(0)
    })
  })
})
