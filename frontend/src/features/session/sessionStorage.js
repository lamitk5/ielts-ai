export const SESSION_STORAGE_KEY_PREFIX = 'ielts_session_'
export const CURRENT_SESSION_VERSION = 1

const ALLOWED_SNAPSHOT_KEYS = new Set([
  'skill',
  'setId',
  'currentQuestionId',
  'splitRatio',
  'activeMobileTab',
  'draftId',
  'draftVersion',
])

export function saveSessionSnapshot(userId, snapshot) {
  if (!userId || typeof userId !== 'string' || !snapshot || typeof snapshot !== 'object') {
    return
  }

  try {
    const clean = {
      version: CURRENT_SESSION_VERSION,
    }

    for (const [key, value] of Object.entries(snapshot)) {
      if (ALLOWED_SNAPSHOT_KEYS.has(key) && value !== undefined) {
        clean[key] = value
      }
    }

    clean.updatedAt = Date.now()
    localStorage.setItem(`${SESSION_STORAGE_KEY_PREFIX}${userId}`, JSON.stringify(clean))
  } catch {
    // ignore quota/storage errors
  }
}

export function loadSessionSnapshot(userId) {
  if (!userId || typeof userId !== 'string') {
    return null
  }

  try {
    const raw = localStorage.getItem(`${SESSION_STORAGE_KEY_PREFIX}${userId}`)
    if (!raw) return null

    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object' || parsed.version !== CURRENT_SESSION_VERSION) {
      return null
    }

    return parsed
  } catch {
    return null
  }
}

export function clearSessionSnapshot(userId) {
  if (!userId || typeof userId !== 'string') {
    return
  }

  try {
    localStorage.removeItem(`${SESSION_STORAGE_KEY_PREFIX}${userId}`)
  } catch {
    // ignore storage errors
  }
}
