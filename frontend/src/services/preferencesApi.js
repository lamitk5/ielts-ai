import { isCompletePreferences, normalizePreferences } from '../features/preferences/preferenceSchema'

const wireEnums = {
  themeMode: { system: 'SYSTEM', light: 'LIGHT', dark: 'DARK' },
  accentPreset: Object.fromEntries(['gold', 'sapphire', 'emerald', 'burgundy', 'violet', 'slate'].map((value) => [value, value.toUpperCase()])),
  fontScale: { small: 'SMALL', default: 'DEFAULT', large: 'LARGE' },
  density: { spacious: 'COMFORTABLE', default: 'DEFAULT', compact: 'COMPACT' },
  reduceMotion: { system: 'SYSTEM', reduce: 'REDUCED', allow: 'ALLOWED' },
  language: { vi: 'VI', en: 'EN' },
}

function toWire(preferences) {
  const normalized = normalizePreferences(preferences)
  return Object.fromEntries(Object.entries(normalized)
    .map(([key, value]) => [key, wireEnums[key]?.[value] ?? value]))
}

function fromWire(record) {
  const preferences = { ...record }
  for (const [key, values] of Object.entries(wireEnums)) {
    const match = Object.entries(values).find(([, wireValue]) => wireValue === record[key])
    if (match) preferences[key] = match[0]
  }
  if (!isCompletePreferences(preferences)) throw new Error('Invalid preference response')
  return { ...normalizePreferences(preferences), version: record.version }
}

function readToken() {
  try { return JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')?.token } catch { return null }
}

async function request(method, body) {
  const token = readToken()
  if (!token) throw new Error('AUTH_REQUIRED')
  const response = await fetch('/api/user/preferences', {
    method,
    headers: { Authorization: `Bearer ${token}`, ...(body ? { 'Content-Type': 'application/json' } : {}) },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })
  if (response.status === 409) {
    const error = new Error('Preference conflict')
    error.code = 'CONFLICT'
    throw error
  }
  if (!response.ok) throw new Error('Không thể đồng bộ thiết lập.')
  const result = await response.json()
  if (!Number.isSafeInteger(result?.version) || result.version < 0) throw new Error('Invalid preference response')
  return fromWire(result)
}

export function getPreferences() {
  return request('GET')
}

export function savePreferences(preferences, version) {
  return request('PUT', { ...toWire(preferences), version })
}
