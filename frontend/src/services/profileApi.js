function authHeaders() {
  const token = localStorage.getItem('auth_token') || sessionStorage.getItem('auth_token')
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  return headers
}

export async function getProfile() {
  const response = await fetch('/api/me/profile', {
    headers: authHeaders(),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => null)
    throw new Error(error?.message || 'Không thể tải thông tin cá nhân.')
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
    const error = await response.json().catch(() => null)
    throw new Error(error?.message || 'Không thể cập nhật hồ sơ.')
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
    const error = await response.json().catch(() => null)
    throw new Error(error?.message || 'Không thể đổi mật khẩu.')
  }
  return response.json()
}
