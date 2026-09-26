import { normalizePreferences } from '../features/preferences/preferenceSchema'

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
  return { ...normalizePreferences(result), version: result.version }
}

export function getPreferences() {
  return request('GET')
}

export function savePreferences(preferences, version) {
  return request('PUT', { ...normalizePreferences(preferences), version })
}
