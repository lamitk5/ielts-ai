import { useReducer, useEffect, useCallback, useRef } from 'react'
import { getLearningDashboardData } from '../../services/learningIntelligenceApi'
import {
  normalizeLearningProfile,
  normalizeSkillRecords,
  normalizeMistakes,
  normalizeRoadmap,
  normalizeLearningActivity,
} from './learningIntelligenceAdapters'

export const LEARNING_STATE_STATUS = {
  IDLE: 'IDLE',
  LOADING: 'LOADING',
  READY: 'READY',
  EMPTY: 'EMPTY',
  ERROR: 'ERROR',
}

const initialState = {
  status: LEARNING_STATE_STATUS.IDLE,
  data: null,
  error: null,
}

export function learningIntelligenceReducer(state = initialState, action) {
  switch (action.type) {
    case 'FETCH_START':
      return { ...state, status: LEARNING_STATE_STATUS.LOADING, error: null }
    case 'FETCH_SUCCESS': {
      const payload = action.payload
      const hasContent = Boolean(
        payload?.profile ||
        (payload?.skills && payload.skills.length > 0) ||
        (payload?.mistakes && payload.mistakes.length > 0) ||
        payload?.roadmap ||
        (payload?.activity && payload.activity.length > 0)
      )
      const hasData = payload?.hasData ?? hasContent
      const isEmpty = !payload || !hasData
      return {
        ...state,
        status: isEmpty ? LEARNING_STATE_STATUS.EMPTY : LEARNING_STATE_STATUS.READY,
        data: payload,
        error: null,
      }
    }
    case 'FETCH_ERROR':
      return {
        ...state,
        status: LEARNING_STATE_STATUS.ERROR,
        error: action.payload || 'Lỗi khi tải dữ liệu học tập',
      }
    default:
      return state
  }
}

export function useLearningIntelligence({ enabled = true } = {}) {
  const [state, dispatch] = useReducer(learningIntelligenceReducer, initialState)
  const abortControllerRef = useRef(null)

  const fetchData = useCallback(async () => {
    if (!enabled) return

    if (abortControllerRef.current) {
      abortControllerRef.current.abort()
    }

    const controller = new AbortController()
    abortControllerRef.current = controller

    dispatch({ type: 'FETCH_START' })

    try {
      const result = await getLearningDashboardData(controller.signal)
      dispatch({ type: 'FETCH_SUCCESS', payload: result })
    } catch (err) {
      if (err?.name === 'AbortError') return
      dispatch({ type: 'FETCH_ERROR', payload: err?.message || 'Không thể tải dữ liệu học tập' })
    }
  }, [enabled])

  useEffect(() => {
    if (enabled) {
      fetchData()
    }
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort()
      }
    }
  }, [enabled, fetchData])

  const profile = normalizeLearningProfile(state.data?.profile)
  const skills = normalizeSkillRecords(state.data?.skills)
  const mistakes = normalizeMistakes(state.data?.mistakes)
  const roadmap = normalizeRoadmap(state.data?.roadmap)
  const activity = normalizeLearningActivity(state.data?.activity)

  return {
    status: state.status,
    data: state.data,
    profile,
    skills,
    mistakes,
    roadmap,
    activity,
    error: state.error,
    refresh: fetchData,
  }
}

export default useLearningIntelligence
