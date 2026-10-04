function authHeaders() {
  let sessionToken = null
  try {
    sessionToken = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')?.token
  } catch {
    sessionToken = null
  }
  const token = sessionToken || localStorage.getItem('auth_token') || sessionStorage.getItem('auth_token')
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  return headers
}

async function readError(response, fallback) {
  const error = await response.json().catch(() => null)
  return new Error(error?.error?.message || error?.message || fallback)
}

export async function getProfile() {
  const response = await fetch('/api/me/profile', {
    headers: authHeaders(),
  })
  if (!response.ok) {
    throw await readError(response, 'Không thể tải thông tin cá nhân.')
  }
  return response.json()
}

export async function updateProfile(data) {
  const response = await fetch('/api/me/profile', {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(data),
  })
  if (!response.ok) {
    throw await readError(response, 'Không thể cập nhật hồ sơ.')
  }
  return response.json()
}

export async function changePassword(currentPassword, newPassword) {
  const response = await fetch('/api/me/password', {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ currentPassword, newPassword }),
  })
  if (!response.ok) {
    throw await readError(response, 'Không thể đổi mật khẩu.')
  }
  return response.json()
}
