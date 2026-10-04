import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { describe, expect, test, vi, beforeEach } from 'vitest'
import AdminPracticeGeneratorPage from '../pages/AdminPracticeGeneratorPage'
import AdminPracticeReviewPage from '../pages/AdminPracticeReviewPage'
import { practiceGeneratorApi } from '../features/practice-generator/practiceGeneratorApi'
import { AuthProvider } from '../features/auth/AuthProvider'

vi.mock('../features/practice-generator/practiceGeneratorApi')

describe('AdminGeneratorFlowIntegration', () => {
  const mockSetId = 'set-e2e-12345678'

  const mockPayload = {
    practiceSet: {
      id: mockSetId,
      title: 'Ecosystem Resilience in Coral Reefs',
      skill: 'READING',
      state: 'PENDING_REVIEW',
      createdAt: new Date().toISOString(),
    },
    currentVersion: {
      id: 'v-1',
      versionNumber: 1,
      passageContent: JSON.stringify({
        title: 'Ecosystem Resilience in Coral Reefs',
        paragraphs: [
          { id: 'p1', text: 'Coral reefs are among the most biologically diverse marine ecosystems on Earth.' },
          { id: 'p2', text: 'Thermal stress leads to coral bleaching and long term structural degradation.' },
        ],
      }),
      questionsPayload: JSON.stringify([
        {
          id: 'q1',
          taskType: 'TRUE_FALSE_NOT_GIVEN',
          prompt: 'Coral reefs host significant marine biological diversity.',
          options: ['TRUE', 'FALSE', 'NOT GIVEN'],
          answerKey: 'TRUE',
          evidenceSpan: 'among the most biologically diverse marine ecosystems',
          explanation: 'Paragraph 1 confirms high diversity.',
        },
      ]),
    },
    validationResults: [
      {
        validatorName: 'SIMILARITY_VALIDATOR',
        status: 'PASS',
        findings: '[]',
      },
      {
        validatorName: 'STRUCTURAL_VALIDATOR',
        status: 'PASS',
        findings: '[]',
      },
      {
        validatorName: 'ANSWER_KEY_VALIDATOR',
        status: 'PASS',
        findings: '[]',
      },
    ],
    versionHistory: [{ versionNumber: 1, createdAt: new Date().toISOString() }],
  }

  beforeEach(() => {
    vi.clearAllMocks()
    practiceGeneratorApi.listJobs = vi.fn().mockResolvedValue([])
    practiceGeneratorApi.listSources = vi.fn().mockResolvedValue([
      { id: 'src-1', title: 'Coral Reef Research', rightsStatus: 'APPROVED' },
    ])
    practiceGeneratorApi.listBlueprints = vi.fn().mockResolvedValue([
      { id: 'bp-1', title: 'Standard Reading Blueprint', targetBand: '7.5' },
    ])
    practiceGeneratorApi.listSets = vi.fn().mockResolvedValue([
      {
        id: mockSetId,
        title: 'Ecosystem Resilience in Coral Reefs',
        skill: 'READING',
        state: 'PENDING_REVIEW',
        createdAt: new Date().toISOString(),
      },
    ])
    practiceGeneratorApi.getSetReviewPayload = vi.fn().mockResolvedValue(mockPayload)
    practiceGeneratorApi.submitReview = vi.fn().mockResolvedValue({
      action: 'APPROVE',
      resultingState: 'APPROVED',
      message: 'Set published as reading-synth-12345678',
    })
  })

  test('full navigation flow from dashboard to split-screen review and approval', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/practice-generator']}>
        <AuthProvider>
          <Routes>
            <Route path="/admin/practice-generator" element={<AdminPracticeGeneratorPage />} />
            <Route path="/admin/practice-generator/sets/:setId" element={<AdminPracticeReviewPage />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    )

    // 1. Verify dashboard renders
    expect(screen.getByText(/Trình tạo bài luyện bằng AI/i)).toBeInTheDocument()
    expect(await screen.findByText('Ecosystem Resilience in Coral Reefs')).toBeInTheDocument()

    // 2. Mở không gian đánh giá
    const reviewLink = screen.getByRole('link', { name: /Mở đánh giá/i })
    fireEvent.click(reviewLink)

    // 3. Verify Review workspace loaded
    expect(await screen.findByText(/Kiểm định tương đồng & Độ mới/i)).toBeInTheDocument()
    expect(screen.getByText(/Coral reefs are among the most biologically diverse/i)).toBeInTheDocument()
    expect(screen.getByText('Coral reefs host significant marine biological diversity.')).toBeInTheDocument()

    // 4. Click Approve
    const approveBtn = screen.getByRole('button', { name: /Phê duyệt & Xuất bản/i })
    fireEvent.click(approveBtn)

    await waitFor(() => {
      expect(practiceGeneratorApi.submitReview).toHaveBeenCalledWith(
        mockSetId,
        expect.objectContaining({ action: 'APPROVE' })
      )
    })
  })
})
