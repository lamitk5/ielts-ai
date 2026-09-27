export async function saveSpeakingAttempt(promptId, transcript, audioFilename = null) {
  const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
  const response = await fetch('/api/practice/speaking/attempts', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
    },
    body: JSON.stringify({ promptId, transcript, audioFilename }),
  })
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.error?.message ?? 'Không thể lưu câu trả lời.')
  return body
}

export async function getSpeakingAttempts() {
  const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
  const response = await fetch('/api/practice/speaking/attempts', {
    headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {},
  })
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.error?.message ?? 'Không thể tải lịch sử Speaking.')
  return body
}

async function speakingAttemptRequest(path, method, body) {
  const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
  const response = await fetch(path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })
  const result = await response.json().catch(() => null)
  if (!response.ok) throw new Error(result?.error?.message ?? 'Không thể lưu attempt Speaking.')
  return { ...result, attemptId: result?.attemptId ?? result?.id }
}

export function startSpeakingAttempt(promptId) {
  return speakingAttemptRequest('/api/practice/speaking/attempts/start', 'POST', { promptId })
}

export function saveSpeakingAttemptDraft(attemptId, transcript) {
  return speakingAttemptRequest(`/api/practice/speaking/attempts/${attemptId}/draft`, 'PUT', { transcript })
}

export function submitSpeakingAttempt(attemptId, transcript) {
  return speakingAttemptRequest(`/api/practice/speaking/attempts/${attemptId}/submit`, 'POST', { transcript })
}
