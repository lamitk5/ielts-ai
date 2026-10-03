const SESSION_KEY = 'ielts-ai-tutor.session'

function sessionHeaders() {
  try {
    const session = JSON.parse(localStorage.getItem(SESSION_KEY) ?? 'null')
    return session?.token ? { Authorization: `Bearer ${session.token}` } : {}
  } catch { return {} }
}

async function request(path, options = {}) {
  const response = await fetch(path, { ...options, headers: { Accept: 'application/json', ...sessionHeaders(), ...(options.headers ?? {}) } })
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.error?.message ?? body?.message ?? 'Không thể tải dữ liệu quản trị.')
  return body
}

const query = (params) => {
  const search = new URLSearchParams()
  Object.entries(params ?? {}).forEach(([key, value]) => { if (value !== undefined && value !== '') search.set(key, value) })
  return search.toString() ? `?${search}` : ''
}

export const adminPortalApi = {
  overview: () => request('/api/admin/overview'),
  learners: (params) => request(`/api/admin/learners${query(params)}`),
  practices: (params) => request(`/api/admin/practices${query(params)}`),
  reviews: () => request('/api/admin/reviews'),
  resolveReport: (id) => request(`/api/admin/reports/${encodeURIComponent(id)}/resolve`, { method: 'POST' }),
  usage: (params) => request(`/api/admin/usage${query(params)}`),
  audit: (params) => request(`/api/admin/audit${query(params)}`),
  prompts: () => request('/api/admin/prompts'),
  createDraft: (body) => request('/api/admin/prompts/drafts', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }),
  activatePrompt: (id) => request(`/api/admin/prompts/versions/${encodeURIComponent(id)}/activate`, { method: 'POST' }),
}
