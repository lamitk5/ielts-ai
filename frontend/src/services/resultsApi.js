function authHeaders() {
  try {
    const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
    return session?.token ? { Authorization: `Bearer ${session.token}` } : {}
  } catch { return {} }
}

async function request(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: { Accept: 'application/json', ...authHeaders(), ...(options.headers ?? {}) },
  })
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.message ?? body?.error?.message ?? 'Không thể tải kết quả.')
  return body
}

export function getLearnerResult(submissionId) {
  return request(`/api/results/submissions/${encodeURIComponent(submissionId)}`)
}

export function getSubmissionHistory({ skill, status, page = 0, size = 20 } = {}) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (skill) params.set('skill', skill)
  if (status) params.set('status', status)
  return request(`/api/me/submissions?${params.toString()}`)
}

export function getSubmissionReviews(submissionId) {
  return request(`/api/results/submissions/${encodeURIComponent(submissionId)}/reviews`)
}

export function getAdminSubmissions({ skill, status, page = 0, size = 20 } = {}) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (skill) params.set('skill', skill)
  if (status) params.set('status', status)
  return request(`/api/admin/submissions?${params.toString()}`)
}

export function getAdminSubmissionResult(submissionId) {
  return request(`/api/admin/submissions/${encodeURIComponent(submissionId)}/result`)
}

export function submitSubmissionReview(submissionId, review) {
  return request(`/api/admin/submissions/${encodeURIComponent(submissionId)}/review`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(review),
  })
}
