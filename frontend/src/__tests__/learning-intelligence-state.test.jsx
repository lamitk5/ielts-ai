import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import {
  LEARNING_STATE_STATUS,
  learningIntelligenceReducer,
  useLearningIntelligence,
} from '../features/learning/learningIntelligenceState'
import * as learningApi from '../services/learningIntelligenceApi'

describe('Task 4: Learning Intelligence State & Hook', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  describe('learningIntelligenceReducer', () => {
    it('handles initial state as IDLE', () => {
      const state = learningIntelligenceReducer(undefined, { type: '@@INIT' })
      expect(state.status).toBe(LEARNING_STATE_STATUS.IDLE)
      expect(state.data).toBeNull()
      expect(state.error).toBeNull()
    })

    it('transitions to LOADING, READY, EMPTY, and ERROR states', () => {
      let state = learningIntelligenceReducer(undefined, { type: 'FETCH_START' })
      expect(state.status).toBe(LEARNING_STATE_STATUS.LOADING)

      const mockData = { profile: { estimatedBand: 6.5 }, hasData: true }
      state = learningIntelligenceReducer(state, { type: 'FETCH_SUCCESS', payload: mockData })
      expect(state.status).toBe(LEARNING_STATE_STATUS.READY)
      expect(state.data).toEqual(mockData)

      state = learningIntelligenceReducer(state, {
        type: 'FETCH_SUCCESS',
        payload: { profile: null, hasData: false },
      })
      expect(state.status).toBe(LEARNING_STATE_STATUS.EMPTY)

      state = learningIntelligenceReducer(state, {
        type: 'FETCH_ERROR',
        payload: 'Network failure',
      })
      expect(state.status).toBe(LEARNING_STATE_STATUS.ERROR)
      expect(state.error).toBe('Network failure')
    })
  })

  describe('useLearningIntelligence hook', () => {
    it('fetches learning intelligence and normalizes server response', async () => {
      vi.spyOn(learningApi, 'getLearningDashboardData').mockResolvedValue({
        profile: { estimatedBand: 7.0, targetBand: 8.0 },
        skills: [{ skill: 'reading', band: 7.5 }],
        mistakes: [{ id: 'm1', skill: 'reading', issue: 'Speed', count: 2 }],
        roadmap: { currentMilestone: 'Skimming' },
        activity: [{ id: 'a1', type: 'PRACTICE_COMPLETED', timestamp: '2026-09-26T08:00:00Z' }],
      })

      const { result } = renderHook(() => useLearningIntelligence({ enabled: true }))

      expect(result.current.status).toBe(LEARNING_STATE_STATUS.LOADING)

      await act(async () => {
        await new Promise((r) => setTimeout(r, 10))
      })

      expect(result.current.status).toBe(LEARNING_STATE_STATUS.READY)
      expect(result.current.profile.estimatedBand).toBe(7.0)
      expect(result.current.skills).toHaveLength(4)
      expect(result.current.mistakes).toHaveLength(1)
    })

    it('transitions to EMPTY state when server returns empty dashboard without inventing data', async () => {
      vi.spyOn(learningApi, 'getLearningDashboardData').mockResolvedValue(null)

      const { result } = renderHook(() => useLearningIntelligence({ enabled: true }))

      await act(async () => {
        await new Promise((r) => setTimeout(r, 10))
      })

      expect(result.current.status).toBe(LEARNING_STATE_STATUS.EMPTY)
      expect(result.current.profile.estimatedBand).toBeNull()
      expect(result.current.profile.bandLabel).toBe('Chưa đủ dữ liệu')
    })

    it('handles API error without crashing and sets ERROR status', async () => {
      vi.spyOn(learningApi, 'getLearningDashboardData').mockRejectedValue(new Error('Server error'))

      const { result } = renderHook(() => useLearningIntelligence({ enabled: true }))

      await act(async () => {
        await new Promise((r) => setTimeout(r, 10))
      })

      expect(result.current.status).toBe(LEARNING_STATE_STATUS.ERROR)
      expect(result.current.error).toBe('Server error')
    })
  })
})
