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

function sessionHeaders() {
  const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
  return {
    'Content-Type': 'application/json',
    ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
  }
}

async function readAttemptResponse(response, fallback) {
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.error?.message ?? fallback)
  return body
}

export async function startPracticeAttempt(skill, setId, practiceVersion = `${setId}:v1`, idempotencyKey = `${skill}:${setId}`) {
  const response = await fetch('/api/attempts', {
    method: 'POST',
    headers: sessionHeaders(),
    body: JSON.stringify({ practiceId: setId, practiceVersion, skill, idempotencyKey }),
  })
  return readAttemptResponse(response, 'Không thể bắt đầu phiên luyện tập.')
}

export async function savePracticeAnswers(attemptId, answers) {
  const response = await fetch(`/api/attempts/${attemptId}/answers`, {
    method: 'PUT',
    headers: sessionHeaders(),
    body: JSON.stringify({ answers }),
  })
  return readAttemptResponse(response, 'Không thể lưu tiến độ bài làm.')
}

export async function submitReadingAttempt(skill, setId, attemptId, answers, idempotencyKey = `${skill}:${setId}`) {
  const response = await fetch(`/api/practice/${skill}/attempts/${attemptId}/submit`, {
    method: 'POST',
    headers: sessionHeaders(),
    body: JSON.stringify({ answers, idempotencyKey }),
  })
  return readAttemptResponse(response, 'Không thể nộp bài Reading.')
}

export async function fetchReadingAttemptResult(skill, attemptId) {
  const response = await fetch(`/api/practice/${skill}/attempts/${attemptId}/result`, {
    headers: sessionHeaders(),
  })
  return readAttemptResponse(response, 'Không thể tải kết quả Reading.')
}
