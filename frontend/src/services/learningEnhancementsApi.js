function authHeaders() {
  try { const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null'); return session?.token ? { Authorization: `Bearer ${session.token}` } : {} } catch { return {} }
}
async function request(path) {
  const response = await fetch(path, { headers: { Accept: 'application/json', ...authHeaders() } })
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.message ?? body?.error?.message ?? 'Không thể tải dữ liệu học tập.')
  return body
}
export function getStudyPlan() { return request('/api/study-plan') }
export function getLearningAnalytics() { return request('/api/analytics') }
