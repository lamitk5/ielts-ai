function authHeaders() {
  const token = localStorage.getItem('auth_token') || sessionStorage.getItem('auth_token')
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  return headers
}

export async function savePractice(publishedSetId) {
  const response = await fetch(`/api/me/saved-practices/${encodeURIComponent(publishedSetId)}`, {
    method: 'POST',
    headers: authHeaders(),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => null)
    throw new Error(error?.message || 'Không thể lưu bài luyện.')
  }
  return response.json()
}

export async function unsavePractice(publishedSetId) {
  const response = await fetch(`/api/me/saved-practices/${encodeURIComponent(publishedSetId)}`, {
    method: 'DELETE',
    headers: authHeaders(),
  })
  if (!response.ok && response.status !== 404) {
    throw new Error('Không thể bỏ lưu bài luyện.')
  }
  return true
}

export async function listSavedPractices(options = {}) {
  const { skill, page = 0, size = 20 } = options
  const params = new URLSearchParams()
  if (skill) params.set('skill', skill)
  if (page) params.set('page', String(page))
  if (size) params.set('size', String(size))

  const response = await fetch(`/api/me/saved-practices?${params.toString()}`, {
    headers: authHeaders(),
  })
  if (!response.ok) {
    throw new Error('Không thể tải danh sách bài đã lưu.')
  }
  return response.json()
}

export async function checkSavedStatus(publishedSetId) {
  const response = await fetch(`/api/me/saved-practices/${encodeURIComponent(publishedSetId)}/status`, {
    headers: authHeaders(),
  })
  if (!response.ok) {
    return { saved: false }
  }
  return response.json()
}
