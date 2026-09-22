export async function fetchPracticeSet(skill) {
  const response = await fetch(`/api/practice/${skill}/sets`)
  if (!response.ok) throw new Error('Không thể tải bộ đề luyện tập.')
  const sets = await response.json()
  const practiceSet = sets[0]
  if (!practiceSet) return undefined
  return {
    ...practiceSet,
    name: practiceSet.name ?? practiceSet.skill,
    setId: practiceSet.setId ?? practiceSet.id,
  }
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
