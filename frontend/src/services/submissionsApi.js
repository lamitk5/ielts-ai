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
  if (!response.ok) throw new Error(body?.message ?? body?.error?.message ?? 'Không thể cập nhật bài làm.')
  return body
}

export function startCanonicalSubmission({ publishedSetId, skill, idempotencyKey }) {
  return request('/api/submissions', {
    method: 'POST',
    body: JSON.stringify({ publishedSetId, skill, idempotencyKey }),
  })
}

export function getCanonicalSubmission(submissionId) {
  return request(`/api/submissions/${encodeURIComponent(submissionId)}`)
}

export function autosaveCanonicalSubmission(submissionId, payload, expectedRevision, idempotencyKey) {
  return request(`/api/submissions/${encodeURIComponent(submissionId)}/draft`, {
    method: 'PUT',
    body: JSON.stringify({ payload, expectedRevision, idempotencyKey }),
  })
}

export function submitCanonicalSubmission(submissionId, answers, idempotencyKey) {
  return request(`/api/submissions/${encodeURIComponent(submissionId)}/submit`, {
    method: 'POST',
    body: JSON.stringify({ payload: answers, idempotencyKey }),
  })
}
