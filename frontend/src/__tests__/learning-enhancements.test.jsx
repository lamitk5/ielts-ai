import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import VocabularyPage from '../pages/VocabularyPage'
import StudyPlanPage from '../pages/StudyPlanPage'
import AnalyticsPage from '../pages/AnalyticsPage'
import AdminMockTestsPage from '../pages/AdminMockTestsPage'
import * as vocabularyApi from '../services/vocabularyApi'
import * as learningEnhancementsApi from '../services/learningEnhancementsApi'
import * as mockTestApi from '../services/mockTestApi'

vi.mock('../services/vocabularyApi')
vi.mock('../services/learningEnhancementsApi')
vi.mock('../services/mockTestApi')

describe('learning enhancement learner flows', () => {
  beforeEach(() => vi.clearAllMocks())

  test('adds a word and reviews it in the vocabulary notebook', async () => {
    vocabularyApi.listVocabulary.mockResolvedValue([])
    vocabularyApi.createVocabularyItem.mockResolvedValue({ id: 'v-1', word: 'resilient', meaning: 'able to recover', status: 'NEW' })
    vocabularyApi.reviewVocabularyItem.mockResolvedValue({ id: 'v-1', word: 'resilient', meaning: 'able to recover', status: 'MASTERED' })

    render(<MemoryRouter><VocabularyPage /></MemoryRouter>)
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Sổ tay từ vựng' })).toBeInTheDocument())
    fireEvent.change(screen.getByLabelText('Từ mới'), { target: { value: 'resilient' } })
    fireEvent.change(screen.getByLabelText('Nghĩa'), { target: { value: 'able to recover' } })
    fireEvent.click(screen.getByRole('button', { name: 'Thêm từ' }))
    await waitFor(() => expect(vocabularyApi.createVocabularyItem).toHaveBeenCalled())
    expect(screen.getByText('resilient')).toBeInTheDocument()
  })

  test('renders truthful study plan and analytics without inventing missing data', async () => {
    learningEnhancementsApi.getStudyPlan.mockResolvedValue({ targetBand: 7, currentEstimatedBand: null, daysRemaining: 72, recommendations: [] })
    learningEnhancementsApi.getLearningAnalytics.mockResolvedValue({ skills: [], questionTypes: [], hasEvidence: false })
    render(<MemoryRouter><StudyPlanPage /><AnalyticsPage /></MemoryRouter>)
    await waitFor(() => expect(screen.getByText('Chưa có đề xuất từ dữ liệu luyện tập.')).toBeInTheDocument())
    expect(screen.getByText('Hoàn thành thêm bài luyện để mở khóa xu hướng.')).toBeInTheDocument()
  })

  test('exposes all four configurable sections in admin mock management', async () => {
    mockTestApi.listAdminMockTests.mockResolvedValue([])

    render(<MemoryRouter><AdminMockTestsPage /></MemoryRouter>)

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Quản lý bài thi thử' })).toBeInTheDocument())
    expect(screen.getAllByLabelText('Kỹ năng')).toHaveLength(4)
    expect(screen.getAllByRole('option', { name: 'SPEAKING' })).toHaveLength(4)
  })
})
