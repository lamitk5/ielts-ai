import { describe, it, expect } from 'vitest'
import {
  MEANINGFUL_EVENT_TYPES,
  isMeaningfulLearningEvent,
  getEventDateKey,
  calculateMeaningfulStreak,
} from '../features/learning/streakRules'

describe('Task 6: Streak Rules', () => {
  describe('isMeaningfulLearningEvent', () => {
    it('accepts only genuine learning completion events', () => {
      expect(isMeaningfulLearningEvent({ type: 'PRACTICE_COMPLETED' })).toBe(true)
      expect(isMeaningfulLearningEvent({ type: 'WRITING_SUBMITTED' })).toBe(true)
      expect(isMeaningfulLearningEvent({ type: 'SPEAKING_SAVED' })).toBe(true)
      expect(isMeaningfulLearningEvent({ type: 'ASSESSMENT_COMPLETED' })).toBe(true)
    })

    it('rejects passive or non-learning events (login, page visit, tutor open, settings)', () => {
      expect(isMeaningfulLearningEvent({ type: 'USER_LOGIN' })).toBe(false)
      expect(isMeaningfulLearningEvent({ type: 'PAGE_VIEW' })).toBe(false)
      expect(isMeaningfulLearningEvent({ type: 'TUTOR_OPENED' })).toBe(false)
      expect(isMeaningfulLearningEvent({ type: 'SETTINGS_CHANGED' })).toBe(false)
      expect(isMeaningfulLearningEvent(null)).toBe(false)
      expect(isMeaningfulLearningEvent({})).toBe(false)
    })
  })

  describe('calculateMeaningfulStreak', () => {
    it('calculates continuous daily streak across distinct days', () => {
      const today = new Date('2026-09-26T12:00:00Z')
      const events = [
        { type: 'PRACTICE_COMPLETED', timestamp: '2026-09-26T08:00:00Z' },
        { type: 'WRITING_SUBMITTED', timestamp: '2026-09-25T14:00:00Z' },
        { type: 'SPEAKING_SAVED', timestamp: '2026-09-24T18:00:00Z' },
      ]

      const result = calculateMeaningfulStreak(events, 'UTC', today)
      expect(result.streakDays).toBe(3)
      expect(result.isActiveToday).toBe(true)
    })

    it('deduplicates multiple events on the same day', () => {
      const today = new Date('2026-09-26T12:00:00Z')
      const events = [
        { type: 'PRACTICE_COMPLETED', timestamp: '2026-09-26T08:00:00Z' },
        { type: 'WRITING_SUBMITTED', timestamp: '2026-09-26T10:00:00Z' },
        { type: 'SPEAKING_SAVED', timestamp: '2026-09-26T20:00:00Z' },
        { type: 'PRACTICE_COMPLETED', timestamp: '2026-09-25T11:00:00Z' },
      ]

      const result = calculateMeaningfulStreak(events, 'UTC', today)
      expect(result.streakDays).toBe(2)
    })

    it('preserves streak if learner practiced yesterday but has not yet practiced today', () => {
      const today = new Date('2026-09-26T12:00:00Z')
      const events = [
        { type: 'PRACTICE_COMPLETED', timestamp: '2026-09-25T14:00:00Z' },
        { type: 'WRITING_SUBMITTED', timestamp: '2026-09-24T14:00:00Z' },
      ]

      const result = calculateMeaningfulStreak(events, 'UTC', today)
      expect(result.streakDays).toBe(2)
      expect(result.isActiveToday).toBe(false)
    })

    it('resets streak to 0 if more than 1 day was missed', () => {
      const today = new Date('2026-09-26T12:00:00Z')
      const events = [
        { type: 'PRACTICE_COMPLETED', timestamp: '2026-09-24T14:00:00Z' }, // missed 25th
      ]

      const result = calculateMeaningfulStreak(events, 'UTC', today)
      expect(result.streakDays).toBe(0)
    })

    it('ignores passive login/visit events during streak calculation', () => {
      const today = new Date('2026-09-26T12:00:00Z')
      const events = [
        { type: 'USER_LOGIN', timestamp: '2026-09-26T08:00:00Z' },
        { type: 'PAGE_VIEW', timestamp: '2026-09-25T10:00:00Z' },
        { type: 'TUTOR_OPENED', timestamp: '2026-09-24T11:00:00Z' },
      ]

      const result = calculateMeaningfulStreak(events, 'UTC', today)
      expect(result.streakDays).toBe(0)
      expect(result.isActiveToday).toBe(false)
    })
  })
})
