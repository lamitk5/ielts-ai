export async function searchPractice(query, options = {}) {
  const { skill, type, page = 0, size = 20 } = options
  const params = new URLSearchParams()
  params.set('q', query)
  if (skill) params.set('skill', skill)
  if (type) params.set('type', type)
  if (page) params.set('page', String(page))
  if (size) params.set('size', String(size))

  const token = localStorage.getItem('auth_token') || sessionStorage.getItem('auth_token')
  const headers = {}
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`/api/practice/search?${params.toString()}`, { headers })
  const payload = await response.json().catch(() => null)
  if (!response.ok || !Array.isArray(payload)) {
    throw new Error('Không thể tải kết quả tìm kiếm lúc này.')
  }
  return payload
}
