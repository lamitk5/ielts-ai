import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { SkillEnergyGrid } from '../components/learning/SkillEnergyGrid'
import { StreakCard } from '../components/learning/StreakCard'

describe('Task 6: Skill Energy & Motivation Presentation', () => {
  describe('SkillEnergyGrid', () => {
    it('renders four-skill energy bars with estimated band labels', () => {
      const skills = [
        { skill: 'reading', name: 'Reading', band: 7.5, bandLabel: 'Band ước lượng 7.5', hasSufficientData: true },
        { skill: 'listening', name: 'Listening', band: 7.0, bandLabel: 'Band ước lượng 7.0', hasSufficientData: true },
        { skill: 'writing', name: 'Writing', band: 6.0, bandLabel: 'Band ước lượng 6.0', hasSufficientData: true },
        { skill: 'speaking', name: 'Speaking', band: null, bandLabel: 'Chưa đủ dữ liệu', hasSufficientData: false },
      ]

      render(<SkillEnergyGrid skills={skills} />)

      expect(screen.getByText('Năng lượng & Độ vững kỹ năng')).toBeInTheDocument()
      expect(screen.getByText('Reading')).toBeInTheDocument()
      expect(screen.getByText('Listening')).toBeInTheDocument()
      expect(screen.getByText('Writing')).toBeInTheDocument()
      expect(screen.getByText('Speaking')).toBeInTheDocument()

      expect(screen.getByText('Band ước lượng 7.5')).toBeInTheDocument()
      expect(screen.getByText('Chưa đủ dữ liệu')).toBeInTheDocument()
    })

    it('renders neutral state across all 4 skills when no skill evidence is present', () => {
      render(<SkillEnergyGrid skills={[]} />)

      expect(screen.getByText('Năng lượng & Độ vững kỹ năng')).toBeInTheDocument()
      const neutralLabels = screen.getAllByText('Chưa đủ dữ liệu')
      expect(neutralLabels.length).toBe(4)
    })
  })

  describe('StreakCard', () => {
    it('renders active streak count and motivation message', () => {
      const streakInfo = {
        streakDays: 5,
        isActiveToday: true,
      }

      render(<StreakCard streakInfo={streakInfo} />)

      expect(screen.getByText('Chuỗi học tập')).toBeInTheDocument()
      expect(screen.getByText('5')).toBeInTheDocument()
      expect(screen.getByText(/5 ngày liên tiếp có hoạt động luyện tập/i)).toBeInTheDocument()
    })

    it('renders zero streak encouragement when no streak active', () => {
      const streakInfo = {
        streakDays: 0,
        isActiveToday: false,
      }

      render(<StreakCard streakInfo={streakInfo} />)

      expect(screen.getByText('Chuỗi học tập')).toBeInTheDocument()
      expect(screen.getByText('0')).toBeInTheDocument()
      expect(screen.getByText(/Hoàn thành bài luyện tập hôm nay để bắt đầu chuỗi/i)).toBeInTheDocument()
    })
  })
})
