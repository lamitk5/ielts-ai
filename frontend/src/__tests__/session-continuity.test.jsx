import { describe, expect, test, beforeEach, afterEach, vi } from 'vitest'
import {
  saveSessionSnapshot,
  loadSessionSnapshot,
  clearSessionSnapshot,
  SESSION_STORAGE_KEY_PREFIX,
} from '../features/session/sessionStorage'
import { validateSessionSnapshot } from '../features/session/sessionContinuity'

describe('Task 8: Session Storage & Namespacing', () => {
  const userId = 'user-uuid-1234'

  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  test('saves and loads version-namespaced session snapshot for authenticated user', () => {
    const snapshot = {
      skill: 'reading',
      setId: 'reading-set-01',
      currentQuestionId: 'q2',
      splitRatio: 50,
      activeMobileTab: 1,
    }

    saveSessionSnapshot(userId, snapshot)
    const storedRaw = localStorage.getItem(`${SESSION_STORAGE_KEY_PREFIX}${userId}`)
    expect(storedRaw).toBeDefined()

    const loaded = loadSessionSnapshot(userId)
    expect(loaded).toEqual({
      version: 1,
      skill: 'reading',
      setId: 'reading-set-01',
      currentQuestionId: 'q2',
      splitRatio: 50,
      activeMobileTab: 1,
      updatedAt: expect.any(Number),
    })
  })

  test('strips sensitive fields (tokens, answer keys, private tutor history)', () => {
    const dirtySnapshot = {
      skill: 'writing',
      setId: 'task-1',
      token: 'secret-auth-token',
      correctAnswer: 'A',
      answers: { q1: 'A' },
      privateTutorHistory: [{ role: 'user', content: 'private' }],
      draftId: 'draft-99',
      draftVersion: 3,
    }

    saveSessionSnapshot(userId, dirtySnapshot)
    const loaded = loadSessionSnapshot(userId)

    expect(loaded.token).toBeUndefined()
    expect(loaded.correctAnswer).toBeUndefined()
    expect(loaded.answers).toBeUndefined()
    expect(loaded.privateTutorHistory).toBeUndefined()
    expect(loaded.draftId).toBe('draft-99')
    expect(loaded.draftVersion).toBe(3)
  })

  test('clears session snapshot on clearSessionSnapshot', () => {
    saveSessionSnapshot(userId, { skill: 'reading', setId: 'reading-01' })
    expect(loadSessionSnapshot(userId)).not.toBeNull()

    clearSessionSnapshot(userId)
    expect(loadSessionSnapshot(userId)).toBeNull()
  })
})

describe('Task 8: Session Continuity Validator', () => {
  test('validates and accepts snapshot matching server-owned exercises', () => {
    const snapshot = {
      version: 1,
      skill: 'reading',
      setId: 'reading-academic-01',
      currentQuestionId: 'q1',
      splitRatio: 50,
    }

    const validated = validateSessionSnapshot(snapshot, {
      validSetIds: ['reading-academic-01', 'reading-academic-02'],
    })

    expect(validated).not.toBeNull()
    expect(validated.setId).toBe('reading-academic-01')
  })

  test('discards snapshot if setId is not in server-owned exercises', () => {
    const snapshot = {
      version: 1,
      skill: 'reading',
      setId: 'unauthorized-or-deleted-set',
      currentQuestionId: 'q1',
    }

    const validated = validateSessionSnapshot(snapshot, {
      validSetIds: ['reading-academic-01', 'reading-academic-02'],
    })

    expect(validated).toBeNull()
  })

  test('discards malformed snapshot with invalid version or structure', () => {
    expect(validateSessionSnapshot(null)).toBeNull()
    expect(validateSessionSnapshot({})).toBeNull()
    expect(validateSessionSnapshot({ version: 99, skill: 'reading' })).toBeNull()
    expect(validateSessionSnapshot('invalid json string')).toBeNull()
  })
})
