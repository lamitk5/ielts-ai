function authHeaders() {
  try {
    const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
    return session?.token ? { Authorization: `Bearer ${session.token}` } : {}
  } catch { return {} }
}

async function request(url, options = {}) {
  const response = await fetch(url, { ...options, headers: { Accept: 'application/json', ...authHeaders(), ...(options.headers ?? {}) } })
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.message ?? 'Không thể tải bài đánh giá.')
  return body
}

export const getDiagnosticSession = () => request('/api/me/diagnostic/session')
export const getDiagnosticResult = (id) => request(`/api/me/diagnostic/${encodeURIComponent(id)}/result`)
export const getDiagnosticHistory = () => request('/api/me/diagnostic/history')
export const retakeDiagnostic = (id) => request(`/api/me/diagnostic/session/${encodeURIComponent(id)}/retake`, { method: 'POST' })
export const finishDiagnostic = (id, complete = false) => request(`/api/me/diagnostic/${encodeURIComponent(id)}/finish`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ complete }) })
export const markDiagnosticUnavailable = (id, skill, message) => request(`/api/me/diagnostic/${encodeURIComponent(id)}/sections/${skill}/unavailable`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ message }) })
