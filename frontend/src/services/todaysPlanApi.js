function authHeaders() {
  try { const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null'); return session?.token ? { Authorization: `Bearer ${session.token}` } : {} } catch { return {} }
}
export async function getTodaysPlan(minutes) {
  const query = minutes ? `?minutes=${encodeURIComponent(minutes)}` : ''
  const response = await fetch(`/api/me/today${query}`, { headers: { Accept: 'application/json', ...authHeaders() } })
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.message ?? 'Không thể tải kế hoạch hôm nay.')
  return body
}
