function authHeaders() {
  try {
    const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
    return session?.token ? { Authorization: `Bearer ${session.token}` } : {}
  } catch {
    return {}
  }
}

async function request(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...authHeaders(),
      ...(options.headers ?? {}),
    },
  })
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    const message = body?.message ?? body?.error?.message ?? 'Đã xảy ra lỗi trong bài thi thử.'
    const error = new Error(message)
    error.status = response.status
    error.data = body
    throw error
  }
  return body
}

export function startOrResumeMockSession({ mockTestId } = {}) {
  return request('/api/mock-tests/sessions/start', {
    method: 'POST',
    body: JSON.stringify({ mockTestId }),
  })
}

export function getActiveMockSession() {
  return request('/api/mock-tests/sessions/active')
}

export function getMockSession(sessionId) {
  return request(`/api/mock-tests/sessions/${encodeURIComponent(sessionId)}`)
}

export function executeMockCommand(sessionId, command) {
  return request(`/api/mock-tests/sessions/${encodeURIComponent(sessionId)}/command`, {
    method: 'POST',
    body: JSON.stringify({ command }),
  })
}

export function autosaveMockSectionDraft(sessionId, sectionIndex, { answers, expectedRevision, idempotencyKey }) {
  return request(`/api/mock-tests/sessions/${encodeURIComponent(sessionId)}/sections/${encodeURIComponent(sectionIndex)}/draft`, {
    method: 'POST',
    body: JSON.stringify({ answers, expectedRevision, idempotencyKey }),
  })
}

export function getMockTestResult(sessionId) {
  return request(`/api/mock-tests/sessions/${encodeURIComponent(sessionId)}/result`)
}

export function listMockSessions() {
  return request('/api/mock-tests/sessions')
}
