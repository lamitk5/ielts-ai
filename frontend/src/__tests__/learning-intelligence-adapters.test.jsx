import { describe, it, expect } from 'vitest'
import {
  normalizeLearningProfile,
  normalizeSkillRecords,
  normalizeMistakes,
  normalizeRoadmap,
  normalizeLearningActivity,
} from '../features/learning/learningIntelligenceAdapters'

describe('Task 4: Learning Intelligence Adapters', () => {
  describe('normalizeLearningProfile', () => {
    it('normalizes valid profile and labels estimated band correctly', () => {
      const raw = {
        userId: 'user-1',
        estimatedBand: 6.5,
        targetBand: 7.5,
        targetDate: '2026-12-31',
        daysRemaining: 95,
      }

      const normalized = normalizeLearningProfile(raw)
      expect(normalized.estimatedBand).toBe(6.5)
      expect(normalized.bandLabel).toBe('Band ước lượng')
      expect(normalized.targetBand).toBe(7.5)
      expect(normalized.daysRemaining).toBe(95)
      expect(normalized.hasSufficientData).toBe(true)
    })

    it('returns neutral empty profile when server data is absent or insufficient', () => {
      const normalized = normalizeLearningProfile(null)
      expect(normalized.estimatedBand).toBeNull()
      expect(normalized.bandLabel).toBe('Chưa đủ dữ liệu')
      expect(normalized.hasSufficientData).toBe(false)
    })

    it('does not compute fake scores or bands when band is null or undefined', () => {
      const normalized = normalizeLearningProfile({ userId: 'user-2' })
      expect(normalized.estimatedBand).toBeNull()
      expect(normalized.bandLabel).toBe('Chưa đủ dữ liệu')
      expect(normalized.hasSufficientData).toBe(false)
    })
  })

  describe('normalizeSkillRecords', () => {
    it('normalizes all four first-class skills with equal structure', () => {
      const raw = [
        { skill: 'reading', band: 7.0, practiceCount: 12 },
        { skill: 'listening', band: 6.5, practiceCount: 8 },
        { skill: 'writing', band: 6.0, practiceCount: 4 },
        { skill: 'speaking', band: null, practiceCount: 1 },
      ]

      const normalized = normalizeSkillRecords(raw)
      expect(normalized).toHaveLength(4)
      
      const skills = normalized.map((s) => s.skill)
      expect(skills).toEqual(['reading', 'listening', 'writing', 'speaking'])

      const reading = normalized.find((s) => s.skill === 'reading')
      expect(reading.band).toBe(7.0)
      expect(reading.bandLabel).toBe('Band ước lượng 7.0')
      expect(reading.hasSufficientData).toBe(true)

      const speaking = normalized.find((s) => s.skill === 'speaking')
      expect(speaking.band).toBeNull()
      expect(speaking.bandLabel).toBe('Chưa đủ dữ liệu')
      expect(speaking.hasSufficientData).toBe(false)
    })

    it('supplies all four skills with neutral empty states if raw array is empty or undefined', () => {
      const normalized = normalizeSkillRecords(null)
      expect(normalized).toHaveLength(4)
      normalized.forEach((record) => {
        expect(record.band).toBeNull()
        expect(record.bandLabel).toBe('Chưa đủ dữ liệu')
        expect(record.hasSufficientData).toBe(false)
      })
    })
  })

  describe('normalizeMistakes', () => {
    it('caps common mistakes at 2–4 and preserves server evidence', () => {
      const raw = [
        { id: 'm1', skill: 'reading', issue: 'T/F/NG confusion', count: 5 },
        { id: 'm2', skill: 'writing', issue: 'Task 2 paragraph structure', count: 3 },
        { id: 'm3', skill: 'listening', issue: 'Spelling in Section 1', count: 2 },
        { id: 'm4', skill: 'speaking', issue: 'Part 2 fluency pausing', count: 2 },
        { id: 'm5', skill: 'reading', issue: 'Matching headings', count: 1 },
      ]

      const normalized = normalizeMistakes(raw)
      expect(normalized.length).toBeLessThanOrEqual(4)
      expect(normalized.length).toBeGreaterThanOrEqual(2)
      expect(normalized[0].issue).toBe('T/F/NG confusion')
    })

    it('returns empty array when raw mistakes is empty or absent without inventing fake mistakes', () => {
      const normalized = normalizeMistakes([])
      expect(normalized).toEqual([])

      const normalizedNull = normalizeMistakes(null)
      expect(normalizedNull).toEqual([])
    })
  })

  describe('normalizeRoadmap', () => {
    it('normalizes server roadmap and action points', () => {
      const raw = {
        currentMilestone: 'Nắm vững Reading Matching Headings',
        nextMilestone: 'Luyện tập Writing Task 2 Coherence',
        completionPercent: 45,
        primaryAction: {
          title: 'Luyện tập Reading Passage 2',
          targetRoute: '/practice/reading',
          skill: 'reading',
        },
      }

      const normalized = normalizeRoadmap(raw)
      expect(normalized.currentMilestone).toBe('Nắm vững Reading Matching Headings')
      expect(normalized.completionPercent).toBe(45)
      expect(normalized.primaryAction.targetRoute).toBe('/practice/reading')
      expect(normalized.hasRoadmap).toBe(true)
    })

    it('returns neutral empty roadmap when data is absent', () => {
      const normalized = normalizeRoadmap(null)
      expect(normalized.hasRoadmap).toBe(false)
      expect(normalized.currentMilestone).toBe('Chưa có lộ trình')
      expect(normalized.primaryAction).toBeNull()
    })
  })

  describe('normalizeLearningActivity', () => {
    it('normalizes learning event log and filters malformed events', () => {
      const raw = [
        { id: 'e1', type: 'PRACTICE_COMPLETED', skill: 'reading', timestamp: '2026-09-26T10:00:00Z' },
        { id: 'e2', type: 'WRITING_SUBMITTED', skill: 'writing', timestamp: '2026-09-25T14:30:00Z' },
        { invalid: true },
      ]

      const normalized = normalizeLearningActivity(raw)
      expect(normalized).toHaveLength(2)
      expect(normalized[0].id).toBe('e1')
      expect(normalized[0].type).toBe('PRACTICE_COMPLETED')
    })

    it('handles empty activity gracefully', () => {
      expect(normalizeLearningActivity(null)).toEqual([])
      expect(normalizeLearningActivity([])).toEqual([])
    })
  })
})
