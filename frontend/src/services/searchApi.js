export async function searchPractice(query) {
  const response = await fetch(`/api/practice/search?q=${encodeURIComponent(query)}`)
  const payload = await response.json().catch(() => null)
  if (!response.ok || !Array.isArray(payload)) {
    throw new Error('Không thể tải kết quả tìm kiếm lúc này.')
  }
  return payload
}
