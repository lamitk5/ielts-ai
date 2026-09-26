import { render, screen } from '@testing-library/react'
import { describe, expect, test, vi } from 'vitest'
import PracticeReviewCanvas from '../components/admin/review/PracticeReviewCanvas'

describe('PracticeReviewCanvas', () => {
  const mockPayload = {
    practiceSet: {
      id: 'set-1',
      title: 'Deep Sea Hydrothermal Vents',
      state: 'PENDING_REVIEW',
    },
    currentVersion: {
      id: 'v-1',
      versionNumber: 1,
      passageContent: {
        title: 'Deep Sea Hydrothermal Vents',
        paragraphs: [{ id: 'p1', text: 'Hydrothermal vents were discovered in 1977.' }],
      },
      questionsPayload: [
        {
          id: 'q1',
          taskType: 'MULTIPLE_CHOICE',
          prompt: 'When were hydrothermal vents discovered?',
          options: ['1975', '1977', '1980'],
          answerKey: 'B',
          evidenceSpan: 'discovered in 1977',
        },
      ],
    },
    validationResults: [
      {
        validatorName: 'SIMILARITY_VALIDATOR',
        status: 'PASS',
        findings: [],
      },
      {
        validatorName: 'ANSWER_KEY_VALIDATOR',
        status: 'PASS',
        findings: [],
      },
    ],
    versionHistory: [{ versionNumber: 1, createdAt: new Date().toISOString() }],
  }

  test('renders similarity banner and question evidence', () => {
    render(
      <PracticeReviewCanvas
        payload={mockPayload}
        onApprove={vi.fn()}
        onRequestRevision={vi.fn()}
        onReject={vi.fn()}
        onRegenerateQuestion={vi.fn()}
        onCompareVersions={vi.fn()}
      />
    )

    expect(screen.getByText(/Kiểm định tương đồng & Độ mới/i)).toBeInTheDocument()
    expect(screen.getByText('When were hydrothermal vents discovered?')).toBeInTheDocument()
    expect(screen.getAllByText(/discovered in 1977/i)).not.toHaveLength(0)
    expect(screen.getByText('Báo cáo kiểm định chất lượng đa tầng (2 Tiêu chuẩn)')).toBeInTheDocument()
  })
})
