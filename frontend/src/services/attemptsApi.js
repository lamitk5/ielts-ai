function authHeaders() {
  try {
    const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
    return session?.token ? { Authorization: `Bearer ${session.token}` } : {}
  } catch { return {} }
}

async function request(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...authHeaders(), ...(options.headers ?? {}) },
  })
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.error?.message ?? 'Không thể cập nhật lượt làm bài.')
  return body
}

export function startAttempt({ practiceId, practiceVersion = 'v1', skill, idempotencyKey = `attempt-${practiceId}` }) {
  return request('/api/attempts', { method: 'POST', body: JSON.stringify({ practiceId, practiceVersion, skill, idempotencyKey }) })
}

export function getAttempt(attemptId) { return request(`/api/attempts/${encodeURIComponent(attemptId)}`) }

export function saveAttemptAnswers(attemptId, answers) {
  return request(`/api/attempts/${encodeURIComponent(attemptId)}/answers`, { method: 'PUT', body: JSON.stringify({ answers }) })
}

export function submitAttempt(attemptId, answers, score, total, idempotencyKey) {
  return request(`/api/attempts/${encodeURIComponent(attemptId)}/submit`, {
    method: 'POST',
    body: JSON.stringify({ answers, score, total, idempotencyKey }),
  })
}
