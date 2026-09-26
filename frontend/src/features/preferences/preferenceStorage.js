export const PREFERENCE_STORAGE_KEY = 'ielts-ai-tutor.preferences.v1'
import { DEFAULT_PREFERENCES } from './preferenceDefaults'
import { isCompletePreferences, normalizePreferences } from './preferenceSchema'

const recordVersion = 1
const accountKey = (userId) => `ielts-ai-tutor.preferences.account.${encodeURIComponent(userId)}.v1`

function read(key) {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return null
    const record = JSON.parse(raw)
    if (record?.version !== recordVersion || !isCompletePreferences(record.preferences)) throw new Error('Invalid preferences')
    return { preferences: normalizePreferences(record.preferences), serverVersion: record.serverVersion }
  } catch {
    try { localStorage.removeItem(key) } catch { /* storage is unavailable */ }
    return null
  }
}

function write(key, preferences, serverVersion) {
  try {
    const record = { version: recordVersion, preferences: normalizePreferences(preferences) }
    if (Number.isSafeInteger(serverVersion) && serverVersion >= 0) record.serverVersion = serverVersion
    localStorage.setItem(key, JSON.stringify(record))
    return true
  } catch {
    return false
  }
}

export function readGuestPreferences() {
  return read(PREFERENCE_STORAGE_KEY)?.preferences ?? DEFAULT_PREFERENCES
}

export function writeGuestPreferences(preferences) {
  return write(PREFERENCE_STORAGE_KEY, preferences)
}

export function readAccountPreferenceCache(userId) {
  return userId ? read(accountKey(userId)) : null
}

export function writeAccountPreferenceCache(userId, preferences, serverVersion) {
  return userId ? write(accountKey(userId), preferences, serverVersion) : false
}

export function clearAccountPreferenceCache(userId) {
  if (!userId) return
  try { localStorage.removeItem(accountKey(userId)) } catch { /* storage is unavailable */ }
}
