const SESSION_KEY = 'ielts-ai-tutor.session'

function readSession() {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY) ?? 'null')
  } catch {
    return null
  }
}

async function request(path, options = {}) {
  const session = readSession()
  const response = await fetch(`/api${path}`, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
      ...options.headers,
    },
  })
  const body = response.status === 204 ? null : await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(body?.error?.message ?? 'Không thể hoàn tất yêu cầu.')
  }
  return body
}

export async function login(credentials) {
  const response = await request('/auth/login', { method: 'POST', body: JSON.stringify(credentials) })
  localStorage.setItem(SESSION_KEY, JSON.stringify(response))
  return response
}

export async function register(details) {
  const response = await request('/auth/register', { method: 'POST', body: JSON.stringify(details) })
  localStorage.setItem(SESSION_KEY, JSON.stringify(response))
  return response
}

export async function logout() {
  try {
    await request('/auth/logout', { method: 'POST' })
  } finally {
    localStorage.removeItem(SESSION_KEY)
  }
}

export function getStoredSession() {
  return readSession()
}

export function clearStoredSession() {
  localStorage.removeItem(SESSION_KEY)
}

export { SESSION_KEY }
