import { describe, expect, test } from 'vitest'
import {
  normalizeLearningProfile,
  normalizeSkillRecords,
  normalizeMistakes,
  normalizeRoadmap,
} from '../features/learning/learningIntelligenceAdapters'

describe('adaptive dashboard normalized states', () => {
  test('maps AI2-A skill/profile contracts without inventing missing band data', () => {
    const profile = normalizeLearningProfile({
      userId: 'member-1',
      totalPracticeAttempts: 12,
      totalAnsweredQuestions: 80,
      evidenceState: 'OBSERVATION',
    })
    const skills = normalizeSkillRecords([
      { userId: 'member-1', skill: 'READING', latestBandEstimate: 6.5, evidenceState: 'CONFIRMED' },
      { userId: 'member-1', skill: 'LISTENING', latestBandEstimate: null, evidenceState: 'INSUFFICIENT_DATA' },
    ])

    expect(profile.userId).toBe('member-1')
    expect(profile.estimatedBand).toBeNull()
    expect(profile.bandLabel).toBe('Chưa đủ dữ liệu')
    expect(skills).toHaveLength(4)
    expect(skills.find((item) => item.skill === 'reading')).toMatchObject({ band: 6.5, hasSufficientData: true })
    expect(skills.find((item) => item.skill === 'listening')).toMatchObject({ band: null, hasSufficientData: false })
  })

  test('maps server-owned issue and roadmap records only', () => {
    const mistakes = normalizeMistakes([
      { id: 'issue-1', skill: 'READING', category: 'FALSE_NOT_GIVEN_CONFUSION', occurrenceCount: 3, evidenceState: 'CONFIRMED' },
    ])
    const roadmap = normalizeRoadmap({
      status: 'ACTIVE',
      items: [{ skill: 'READING', learningObjective: 'Phân biệt FALSE và NOT GIVEN', status: 'NOT_STARTED', priority: 1 }],
    })

    expect(mistakes[0]).toMatchObject({ id: 'issue-1', issue: 'FALSE_NOT_GIVEN_CONFUSION', count: 3 })
    expect(roadmap.hasRoadmap).toBe(true)
    expect(roadmap.currentMilestone).toContain('Phân biệt FALSE')
  })
})
