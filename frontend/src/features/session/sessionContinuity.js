import { CURRENT_SESSION_VERSION } from './sessionStorage'

export function validateSessionSnapshot(snapshot, options = {}) {
  if (!snapshot || typeof snapshot !== 'object' || Array.isArray(snapshot)) {
    return null
  }

  if (snapshot.version !== CURRENT_SESSION_VERSION) {
    return null
  }

  const { validSetIds } = options

  if (Array.isArray(validSetIds) && validSetIds.length > 0) {
    if (!snapshot.setId || !validSetIds.includes(snapshot.setId)) {
      return null
    }
  }

  return {
    version: snapshot.version,
    skill: snapshot.skill ?? null,
    setId: snapshot.setId ?? null,
    currentQuestionId: snapshot.currentQuestionId ?? null,
    splitRatio: typeof snapshot.splitRatio === 'number' ? snapshot.splitRatio : 50,
    activeMobileTab: typeof snapshot.activeMobileTab === 'number' ? snapshot.activeMobileTab : 0,
    draftId: snapshot.draftId ?? null,
    draftVersion: snapshot.draftVersion != null ? Number(snapshot.draftVersion) : null,
    updatedAt: snapshot.updatedAt ?? Date.now(),
  }
}
