const TOKEN_KEY = 'rag-admin-token'

export function readAdminToken(storage = window.sessionStorage) {
  return storage.getItem(TOKEN_KEY) ?? ''
}

export function writeAdminToken(token, storage = window.sessionStorage) {
  const normalized = token?.trim() ?? ''
  if (normalized) storage.setItem(TOKEN_KEY, normalized)
  else storage.removeItem(TOKEN_KEY)
  return normalized
}

export class RagAdminApiError extends Error {
  constructor(code, message, status) {
    super(message)
    this.name = 'RagAdminApiError'
    this.code = code
    this.status = status
  }
}

export function createRagAdminApi({ fetchImpl = window.fetch.bind(window), storage = window.sessionStorage } = {}) {
  async function request(path, options = {}) {
    if (!path.startsWith('/api/admin/rag/')) throw new Error('RAG admin requests must be same-origin')
    const token = readAdminToken(storage)
    const response = await fetchImpl(path, {
      ...options,
      headers: { ...(options.headers ?? {}), ...(token ? { 'X-Admin-Token': token } : {}) },
    })
    const payload = await response.json().catch(() => null)
    if (!response.ok) {
      throw new RagAdminApiError(payload?.error?.code ?? `HTTP_${response.status}`, payload?.error?.message ?? 'Không thể tải dữ liệu quản trị.', response.status)
    }
    return payload
  }

  return {
    list: () => request('/api/admin/rag/documents'),
    detail: (id) => request(`/api/admin/rag/documents/${encodeURIComponent(id)}`),
    jobs: () => request('/api/admin/rag/jobs'),
    upload: (metadata, file) => {
      const body = new FormData()
      body.append('file', file)
      body.append('metadata', new Blob([JSON.stringify({ ...metadata, rightsStatus: 'PENDING_REVIEW' })], { type: 'application/json' }))
      return request('/api/admin/rag/documents/upload', { method: 'POST', body })
    },
    action: (id, action, rightsNote) => request(`/api/admin/rag/documents/${encodeURIComponent(id)}/${action}${rightsNote ? `?rightsNote=${encodeURIComponent(rightsNote)}` : ''}`, { method: 'POST' }),
  }
}
