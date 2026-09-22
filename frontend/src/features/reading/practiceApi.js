export async function fetchPracticeSet(skill) {
  const response = await fetch(`/api/practice/${skill}/sets`)
  if (!response.ok) throw new Error('Không thể tải bộ đề luyện tập.')
  const sets = await response.json()
  return sets[0]
}

export async function submitPracticeAttempt(skill, setId, answers) {
  const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
  const response = await fetch(`/api/practice/${skill}/attempts`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
    },
    body: JSON.stringify({ setId, answers }),
  })
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.error?.message ?? 'Không thể lưu kết quả.')
  return body
}
