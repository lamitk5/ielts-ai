import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import MockTestResult from '../components/mock/MockTestResult'

describe('MockTestResult Component', () => {
  const fullResult = {
    sessionId: 'sess-full-1',
    mockTestId: 'mock-academic-full',
    resultStatus: 'READY',
    estimatedOverallBand: 7.5,
    estimatedBandLabel: 'Band ước lượng tổng thể',
    aiDisclaimer: 'Band ước lượng bởi AI — Không phải điểm thi IELTS chính thức.',
    totalDurationSeconds: 10200,
    sectionResults: [
      {
        sectionOrder: 0,
        skill: 'LISTENING',
        practiceId: 'p-lis-1',
        submissionId: 'sub-lis-1',
        sectionStatus: 'COMPLETED',
        learnerResult: { score: 34, total: 40, estimatedBand: 7.5 },
      },
      {
        sectionOrder: 1,
        skill: 'READING',
        practiceId: 'p-read-1',
        submissionId: 'sub-read-1',
        sectionStatus: 'COMPLETED',
        learnerResult: { score: 35, total: 40, estimatedBand: 8.0 },
      },
      {
        sectionOrder: 2,
        skill: 'WRITING',
        practiceId: 'p-wri-1',
        submissionId: 'sub-wri-1',
        sectionStatus: 'COMPLETED',
        learnerResult: {
          estimatedBand: 7.0,
          aiEvaluation: { estimatedBand: 7.0, strengths: ['Good cohesion'] },
        },
      },
      {
        sectionOrder: 3,
        skill: 'SPEAKING',
        practiceId: 'p-spk-1',
        submissionId: 'sub-spk-1',
        sectionStatus: 'COMPLETED',
        learnerResult: {
          humanReview: { overallBand: 7.5, status: 'COMPLETED' },
          speaking: { audioAvailable: true },
        },
      },
    ],
  }

  const partialResult = {
    sessionId: 'sess-partial-1',
    mockTestId: 'mock-academic-full',
    resultStatus: 'PARTIALLY_AVAILABLE',
    estimatedOverallBand: null,
    totalDurationSeconds: 9600,
    sectionResults: [
      {
        sectionOrder: 0,
        skill: 'LISTENING',
        practiceId: 'p-lis-1',
        submissionId: 'sub-lis-1',
        sectionStatus: 'COMPLETED',
        learnerResult: { score: 32, total: 40, estimatedBand: 7.0 },
      },
      {
        sectionOrder: 1,
        skill: 'READING',
        practiceId: 'p-read-1',
        submissionId: 'sub-read-1',
        sectionStatus: 'COMPLETED',
        learnerResult: { score: 34, total: 40, estimatedBand: 7.5 },
      },
      {
        sectionOrder: 2,
        skill: 'WRITING',
        practiceId: 'p-wri-1',
        submissionId: 'sub-wri-1',
        sectionStatus: 'COMPLETED',
        learnerResult: null,
        statusMessage: 'Chưa có đánh giá AI cho phiên bản này.',
      },
      {
        sectionOrder: 3,
        skill: 'SPEAKING',
        practiceId: 'p-spk-1',
        submissionId: 'sub-spk-1',
        sectionStatus: 'COMPLETED',
        learnerResult: null,
        statusMessage: 'Bài nói đang chờ đánh giá thủ công.',
      },
    ],
  }

  it('renders full mock test results with overall band and disclaimer', () => {
    render(<MockTestResult result={fullResult} />)

    expect(screen.getByText('Báo Cáo Tổng Hợp Mock Test')).toBeInTheDocument()
    expect(screen.getAllByText('7.5').length).toBeGreaterThan(0)
    expect(screen.getByText(/Không phải điểm thi IELTS chính thức/i)).toBeInTheDocument()

    expect(screen.getByText('Listening (Nghe)')).toBeInTheDocument()
    expect(screen.getByText('Reading (Đọc)')).toBeInTheDocument()
    expect(screen.getByText('Writing (Viết)')).toBeInTheDocument()
    expect(screen.getByText('Speaking (Nói)')).toBeInTheDocument()
  })

  it('renders partial mock test notice without fabricating overall band', () => {
    render(<MockTestResult result={partialResult} />)

    expect(screen.getByText('Kết quả một phần đang được chấm')).toBeInTheDocument()
    expect(screen.queryByText('Band ước lượng tổng thể')).not.toBeInTheDocument()
    expect(screen.getByText(/Điểm tổng thể.*sẽ chỉ hiển thị khi đầy đủ 4 kỹ năng/i)).toBeInTheDocument()
  })

  it('handles action button callbacks', () => {
    const onRetake = vi.fn()
    const onNavigateCatalog = vi.fn()
    render(<MockTestResult result={fullResult} onRetake={onRetake} onNavigateCatalog={onNavigateCatalog} />)

    fireEvent.click(screen.getByText('Làm lại bài thi thử mới'))
    expect(onRetake).toHaveBeenCalled()

    fireEvent.click(screen.getByText('Quay về danh mục luyện tập'))
    expect(onNavigateCatalog).toHaveBeenCalled()
  })
})
