import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { describe, expect, test, vi, beforeEach } from 'vitest'
import AdminPracticeReviewPage from '../pages/AdminPracticeReviewPage'
import { practiceGeneratorApi } from '../features/practice-generator/practiceGeneratorApi'
import { AuthProvider } from '../features/auth/AuthProvider'

vi.mock('../features/practice-generator/practiceGeneratorApi')

describe('AdminPracticeReviewPage', () => {
  const mockPayload = {
    practiceSet: {
      id: 'set-100',
      title: 'Biodiversity in Tropical Rainforests',
      skill: 'READING',
      state: 'PENDING_REVIEW',
    },
    currentVersion: {
      id: 'v-1',
      versionNumber: 1,
      passageContent: JSON.stringify({
        title: 'Biodiversity in Tropical Rainforests',
        paragraphs: [
          { id: 'p1', text: 'Tropical rainforests harbor more than half of the world plant and animal species.' },
        ],
      }),
      questionsPayload: JSON.stringify([
        {
          id: 'q1',
          taskType: 'TRUE_FALSE_NOT_GIVEN',
          prompt: 'Rainforests contain over fifty percent of global species.',
          options: ['TRUE', 'FALSE', 'NOT GIVEN'],
          answerKey: 'TRUE',
          evidenceSpan: 'harbor more than half of the world plant and animal species',
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
    ],
    versionHistory: [{ versionNumber: 1, createdAt: new Date().toISOString() }],
  }

  beforeEach(() => {
    vi.clearAllMocks()
    practiceGeneratorApi.getSetReviewPayload = vi.fn().mockResolvedValue(mockPayload)
    practiceGeneratorApi.submitReview = vi.fn().mockResolvedValue({ action: 'APPROVE' })
  })

  test('renders split-screen review canvas with passage and questions', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/practice-generator/sets/set-100']}>
        <AuthProvider>
          <Routes>
            <Route path="/admin/practice-generator/sets/:setId" element={<AdminPracticeReviewPage />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    )

    expect(await screen.findAllByText('Biodiversity in Tropical Rainforests')).not.toHaveLength(0)
    expect(screen.getByText(/Tropical rainforests harbor more than half/i)).toBeInTheDocument()
    expect(screen.getByText('Rainforests contain over fifty percent of global species.')).toBeInTheDocument()
    expect(screen.getByText(/Phê duyệt & Xuất bản/i)).toBeInTheDocument()
  })

  test('submits approval review action', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/practice-generator/sets/set-100']}>
        <AuthProvider>
          <Routes>
            <Route path="/admin/practice-generator/sets/:setId" element={<AdminPracticeReviewPage />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    )

    const approveBtn = await screen.findByRole('button', { name: /Phê duyệt & Xuất bản/i })
    fireEvent.click(approveBtn)

    await waitFor(() => {
      expect(practiceGeneratorApi.submitReview).toHaveBeenCalledWith(
        'set-100',
        expect.objectContaining({ action: 'APPROVE' })
      )
    })
  })
})
