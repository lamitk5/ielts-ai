import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test, vi, beforeEach } from 'vitest'
import AdminPracticeGeneratorPage from '../pages/AdminPracticeGeneratorPage'
import { practiceGeneratorApi } from '../features/practice-generator/practiceGeneratorApi'
import { AuthProvider } from '../features/auth/AuthProvider'

vi.mock('../features/practice-generator/practiceGeneratorApi')

describe('AdminPracticeGeneratorPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    practiceGeneratorApi.listJobs = vi.fn().mockResolvedValue([])
    practiceGeneratorApi.listSources = vi.fn().mockResolvedValue([
      { id: 'src-1', title: 'Source 1', rightsStatus: 'APPROVED' },
    ])
    practiceGeneratorApi.listBlueprints = vi.fn().mockResolvedValue([
      { id: 'bp-1', title: 'Reading Blueprint 1', targetBand: '7.5' },
    ])
    practiceGeneratorApi.listSets = vi.fn().mockResolvedValue([
      {
        id: 'set-12345678',
        title: 'Polar Exploration Set',
        skill: 'READING',
        state: 'PENDING_REVIEW',
        createdAt: new Date().toISOString(),
      },
    ])
  })

  test('renders KPI cards and job table', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <AdminPracticeGeneratorPage />
        </AuthProvider>
      </MemoryRouter>
    )

    expect(screen.getByText(/Trình tạo bài luyện bằng AI/i)).toBeInTheDocument()
    expect(await screen.findByText('Polar Exploration Set')).toBeInTheDocument()
    expect(screen.getByText('Chờ biên tập viên duyệt')).toBeInTheDocument()
  })

  test('opens new generation wizard modal on button click', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <AdminPracticeGeneratorPage />
        </AuthProvider>
      </MemoryRouter>
    )

    const createBtn = screen.getByRole('button', { name: /Tạo bộ đề mới/i })
    createBtn.click()

    expect(await screen.findByText('Khởi tạo đề thi IELTS AI mới')).toBeInTheDocument()
  })
})
