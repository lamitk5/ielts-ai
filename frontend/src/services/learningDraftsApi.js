function readToken() {
  try {
    return JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')?.token
  } catch {
    return null
  }
}

export async function getCurrentDraft(skill, referenceId) {
  const token = readToken()
  if (!token) return null
  const response = await fetch(
    `/api/learning/drafts/current?skill=${encodeURIComponent(skill)}&referenceId=${encodeURIComponent(referenceId)}`,
    {
      headers: { Authorization: `Bearer ${token}` },
    },
  )
  if (response.status === 404) return null
  if (!response.ok) throw new Error('Không thể tải bản nháp.')
  return response.json()
}

export async function saveDraft({ skill, referenceId, contentSnapshot, expectedVersion }) {
  const token = readToken()
  if (!token) throw new Error('AUTH_REQUIRED')
  const response = await fetch('/api/learning/drafts', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      skill,
      referenceId,
      contentSnapshot,
      expectedVersion,
    }),
  })
  if (response.status === 409) {
    const error = new Error('Bản nháp đã được cập nhật từ thiết bị khác.')
    error.code = 'VERSION_CONFLICT'
    throw error
  }
  if (!response.ok) {
    const errorBody = await response.json().catch(() => null)
    throw new Error(errorBody?.error?.message ?? 'Không thể lưu bản nháp.')
  }
  return response.json()
}

export async function deleteDraft(draftId) {
  const token = readToken()
  if (!token || !draftId) return
  await fetch(`/api/learning/drafts/${encodeURIComponent(draftId)}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token}` },
  }).catch(() => {})
}
