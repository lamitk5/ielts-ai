import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, test, vi } from 'vitest'
import WritingResultShell from '../components/results/WritingResultShell.jsx'
import WritingVersionHistory from '../components/results/WritingVersionHistory.jsx'

describe('WritingResultShell & WritingVersionHistory UI', () => {
  const mockEvaluation = {
    id: 'eval-1',
    versionId: 'ver-1',
    evaluationVersion: 1,
    overallBandEstimate: 7.0,
    criteria: {
      taskAchievement: 'Good summary of data',
      coherenceCohesion: 'Logical paragraph structure',
      lexicalResource: 'Accurate vocabulary choice',
      grammaticalRangeAccuracy: 'Complex sentence structures',
    },
    strengths: ['Clear overview', 'Well-selected data points'],
    issues: ['Minor typo in paragraph 2'],
    suggestions: ['Vary transition words'],
    priorityImprovements: ['Focus on consistent lexical range'],
    groundingStatus: 'NOT_ENABLED',
    disclaimer: 'Band ước lượng bởi AI — Không phải điểm thi IELTS chính thức.',
    status: 'GRADED',
  }

  const mockVersions = [
    {
      id: 'ver-2',
      versionNumber: 2,
      parentVersionId: 'ver-1',
      wordCount: 180,
      createdAt: '2026-10-02T10:00:00Z',
    },
    {
      id: 'ver-1',
      versionNumber: 1,
      parentVersionId: null,
      wordCount: 155,
      createdAt: '2026-10-02T09:30:00Z',
    },
  ]

  test('renders overall band estimate and disclaimer text honestly', () => {
    render(
      <WritingResultShell
        evaluation={mockEvaluation}
        versions={mockVersions}
        activeVersion={mockVersions[0]}
      />
    )

    expect(screen.getByText(/Band ước lượng bởi AI:/i)).toBeInTheDocument()
    expect(screen.getByText('7.0')).toBeInTheDocument()
    expect(
      screen.getByText(/Band ước lượng bởi AI — Không phải điểm thi IELTS chính thức\./i)
    ).toBeInTheDocument()
    expect(screen.getByText(/Task Achievement \(Task 1\)/i)).toBeInTheDocument()
    expect(screen.getByText(/Good summary of data/i)).toBeInTheDocument()
  })

  test('renders failure state and retry button safely without crashing', () => {
    const onRetry = vi.fn()
    const failedEval = {
      ...mockEvaluation,
      status: 'FAILED',
      disclaimer: 'Đánh giá AI đang tạm gián đoạn; bài của bạn vẫn được lưu an toàn.',
    }

    render(
      <WritingResultShell
        evaluation={failedEval}
        versions={mockVersions}
        activeVersion={mockVersions[0]}
        onRetryEvaluation={onRetry}
      />
    )

    expect(screen.getByText(/Đánh giá AI chưa hoàn tất/i)).toBeInTheDocument()
    const retryBtn = screen.getByRole('button', { name: /Thử lại đánh giá AI/i })
    expect(retryBtn).toBeInTheDocument()
    fireEvent.click(retryBtn)
    expect(onRetry).toHaveBeenCalledTimes(1)
  })

  test('renders version history and handles selection', () => {
    const onSelect = vi.fn()
    render(
      <WritingVersionHistory
        versions={mockVersions}
        activeVersionId="ver-2"
        onSelectVersion={onSelect}
      />
    )

    expect(screen.getByText(/Lịch sử phiên bản \(2\)/i)).toBeInTheDocument()
    const v1Item = screen.getByText('155 từ')
    fireEvent.click(v1Item)
    expect(onSelect).toHaveBeenCalled()
  })
})
