import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { TodaysFocusCard } from '../components/learning/TodaysFocusCard'
import { RoadmapWidget } from '../components/learning/RoadmapWidget'
import { CommonMistakesPanel } from '../components/learning/CommonMistakesPanel'

describe('Task 5: Learning Dashboard Cards', () => {
  describe('TodaysFocusCard', () => {
    it('renders server-backed focus action and single primary CTA', () => {
      const roadmap = {
        hasRoadmap: true,
        primaryAction: {
          title: 'Luyện tập Reading: Matching Headings',
          targetRoute: '/practice/reading',
          skill: 'reading',
        },
      }

      render(
        <MemoryRouter>
          <TodaysFocusCard roadmap={roadmap} />
        </MemoryRouter>
      )

      expect(screen.getByText('Trọng tâm hôm nay')).toBeInTheDocument()
      expect(
        screen.getByText('Luyện tập Reading: Matching Headings')
      ).toBeInTheDocument()
      const cta = screen.getByRole('link', { name: /bắt đầu luyện tập/i })
      expect(cta).toHaveAttribute('href', '/practice/reading')
    })

    it('renders neutral invitation without fake recommendations when roadmap is absent', () => {
      render(
        <MemoryRouter>
          <TodaysFocusCard roadmap={null} />
        </MemoryRouter>
      )

      expect(screen.getByText('Trọng tâm hôm nay')).toBeInTheDocument()
      expect(
        screen.getByText(/Hoàn thành thêm bài luyện để nhận gợi ý cá nhân hóa/i)
      ).toBeInTheDocument()
      const cta = screen.getByRole('link', { name: /luyện tập ngay/i })
      expect(cta).toHaveAttribute('href', '/practice/reading')
    })
  })

  describe('RoadmapWidget', () => {
    it('renders milestones and progress percentage from server roadmap', () => {
      const roadmap = {
        hasRoadmap: true,
        currentMilestone: 'Nắm vững kỹ năng Skimming & Scanning',
        nextMilestone: 'Tăng tốc độ làm bài Reading Passage 3',
        completionPercent: 60,
      }

      render(<RoadmapWidget roadmap={roadmap} />)

      expect(screen.getByText('Lộ trình cá nhân')).toBeInTheDocument()
      expect(screen.getByText('Nắm vững kỹ năng Skimming & Scanning')).toBeInTheDocument()
      expect(screen.getByText(/60%/i)).toBeInTheDocument()
    })

    it('renders neutral state when roadmap is not yet established', () => {
      render(<RoadmapWidget roadmap={null} />)

      expect(screen.getByText('Lộ trình cá nhân')).toBeInTheDocument()
      expect(
        screen.getByText(/Chưa có lộ trình/i)
      ).toBeInTheDocument()
    })
  })

  describe('CommonMistakesPanel', () => {
    it('renders 2–4 high value server-backed mistakes with links to practice', () => {
      const mistakes = [
        {
          id: 'm1',
          skill: 'reading',
          issue: 'Nhầm lẫn giữa False và Not Given',
          recommendation: 'Kiểm tra thông tin đối chiếu kỹ hơn.',
          count: 4,
        },
        {
          id: 'm2',
          skill: 'writing',
          issue: 'Thiếu câu chủ đề ở đoạn thân bài 2',
          recommendation: 'Đặt topic sentence ngay đầu mỗi đoạn.',
          count: 2,
        },
      ]

      render(
        <MemoryRouter>
          <CommonMistakesPanel mistakes={mistakes} title="Điểm cần chú ý" />
        </MemoryRouter>
      )

      expect(screen.getByText('Điểm cần chú ý')).toBeInTheDocument()
      expect(screen.getByText('Nhầm lẫn giữa False và Not Given')).toBeInTheDocument()
      expect(screen.getByText('Thiếu câu chủ đề ở đoạn thân bài 2')).toBeInTheDocument()
      expect(screen.getByText(/4 lần · Reading/i)).toBeInTheDocument()
    })

    it('renders calm empty state when no mistakes recorded without inventing fake data', () => {
      render(
        <MemoryRouter>
          <CommonMistakesPanel mistakes={[]} title="Điểm cần chú ý" />
        </MemoryRouter>
      )

      expect(screen.getByText('Điểm cần chú ý')).toBeInTheDocument()
      expect(
        screen.getByText(/Chưa ghi nhận lỗi sai cần chú ý/i)
      ).toBeInTheDocument()
    })
  })
})
