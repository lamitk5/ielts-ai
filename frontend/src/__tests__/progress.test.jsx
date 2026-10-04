import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test, vi } from 'vitest'
import App from '../App'
import BandRadarChart from '../components/charts/BandRadarChart'
import ExamCountdownCard from '../components/home/ExamCountdownCard'
import ProgressOverviewSection from '../components/home/ProgressOverviewSection'
import { memberDemo } from '../data/homepageMockData'

function renderApp(initialEntry = '/') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <App />
    </MemoryRouter>
  )
}

describe('progress overview', () => {
  test('keeps personal analytics out of the guest homepage', () => {
    renderApp('/')

    const progress = screen.getByRole('region', { name: 'Tiến độ luyện tập của bạn' })

    expect(screen.getByText('Đánh giá trình độ để mở bảng tiến độ cá nhân')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Bắt đầu đánh giá' })).toHaveAttribute(
      'href',
      '/assessment',
    )
    expect(
      within(progress).getByRole('img', { name: 'Xem trước tiến độ theo 4 kỹ năng' }),
    ).toBeInTheDocument()
    expect(within(progress).getByRole('img', { name: 'Xem trước tiến độ theo 4 kỹ năng' }).closest('.progress-guest-preview')).toHaveClass('progress-guest-preview-editorial')
    for (const skill of ['Reading', 'Listening', 'Writing', 'Speaking']) {
      expect(within(progress).getByText(skill)).toBeInTheDocument()
    }
    expect(within(progress).getByRole('status')).toHaveAttribute('aria-busy', 'true')
    expect(screen.queryByText(/Reading.*6\.5/)).not.toBeInTheDocument()
    expect(screen.queryByText('Lỗi thường gặp tuần này')).not.toBeInTheDocument()
  })

  test('renders all member bands, estimated labeling, and common mistakes', () => {
    renderApp('/?demo=member')

    const progress = screen.getByRole('region', { name: 'Tiến độ luyện tập của bạn' })
    expect(progress.querySelector('.progress-member-layout')).toHaveClass('progress-dashboard-editorial')

    expect(within(progress).getByText('Band ước lượng')).toBeInTheDocument()
    for (const item of memberDemo.progress) {
      expect(within(progress).getByText(new RegExp(`${item.skill}.*${item.band}`))).toBeInTheDocument()
    }
    expect(within(progress).getByText('Lỗi thường gặp tuần này')).toBeInTheDocument()
    for (const mistake of memberDemo.mistakes) {
      expect(within(progress).getByText(mistake.label)).toBeInTheDocument()
      expect(within(progress).getByText(new RegExp(`${mistake.frequency} lần`))).toBeInTheDocument()
      expect(within(progress).getByText(mistake.hint)).toBeInTheDocument()
    }
  })

  test('provides an accessible radar summary for every skill', () => {
    render(<BandRadarChart data={memberDemo.progress} />)

    const summary = screen.getByRole('img', { name: /band ước lượng theo kỹ năng/i })
    for (const item of memberDemo.progress) {
      expect(summary).toHaveTextContent(`${item.skill}: ${item.band}`)
    }
  })

  test('clamps future, today, and past exam dates without negative days', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-21T12:00:00'))

    const { rerender } = render(<ExamCountdownCard examDate="2026-09-23" />)
    expect(screen.getByText('2 ngày')).toBeInTheDocument()

    rerender(<ExamCountdownCard examDate="2026-09-21" />)
    expect(screen.getByText('Ngày thi đã đến hoặc đã qua')).toBeInTheDocument()
    expect(screen.queryByText(/-\d+ ngày/)).not.toBeInTheDocument()

    rerender(<ExamCountdownCard examDate="2026-09-20" />)
    expect(screen.getByText('0 ngày')).toBeInTheDocument()
    vi.useRealTimers()
  })

  test('does not invent a target exam date for a newly authenticated member', () => {
    render(<ExamCountdownCard />)

    expect(screen.getByText('Chưa đặt ngày thi')).toBeInTheDocument()
    expect(screen.queryByText('undefined')).not.toBeInTheDocument()
  })

  test('uses the shared skeleton semantics while progress data loads', () => {
    render(<ProgressOverviewSection isAuthenticated state={memberDemo} loading />)

    expect(screen.getByRole('status')).toHaveAttribute('aria-busy', 'true')
  })

  test('keeps empty member progress free of null or fake band values', () => {
    render(
      <MemoryRouter>
        <ProgressOverviewSection
          isAuthenticated
          state={{
            user: { firstName: 'Mai', examDate: null },
            progress: ['Reading', 'Listening', 'Writing', 'Speaking'].map((skill) => ({ skill, band: null })),
            mistakes: [],
          }}
        />
      </MemoryRouter>,
    )

    expect(screen.getByText('Đánh giá trình độ để mở bảng tiến độ cá nhân')).toBeInTheDocument()
    expect(screen.queryByText('null')).not.toBeInTheDocument()
    expect(screen.queryByText('Band ước lượng')).not.toBeInTheDocument()
  })
})
