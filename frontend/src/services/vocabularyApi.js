function authHeaders() {
  try { const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null'); return session?.token ? { Authorization: `Bearer ${session.token}` } : {} } catch { return {} }
}

async function request(path, options = {}) {
  const response = await fetch(path, { ...options, headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...authHeaders(), ...(options.headers ?? {}) } })
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.message ?? body?.error?.message ?? 'Không thể cập nhật sổ tay từ vựng.')
  return body
}

export function listVocabulary({ query = '', status = '' } = {}) {
  const params = new URLSearchParams()
  if (query) params.set('q', query)
  if (status) params.set('status', status)
  return request(`/api/vocabulary${params.toString() ? `?${params}` : ''}`)
}
export function createVocabularyItem(item) { return request('/api/vocabulary', { method: 'POST', body: JSON.stringify(item) }) }
export function updateVocabularyItem(id, item) { return request(`/api/vocabulary/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(item) }) }
export function deleteVocabularyItem(id) { return request(`/api/vocabulary/${encodeURIComponent(id)}`, { method: 'DELETE' }) }
export function reviewVocabularyItem(id, status) { return request(`/api/vocabulary/${encodeURIComponent(id)}/review`, { method: 'POST', body: JSON.stringify({ status }) }) }
