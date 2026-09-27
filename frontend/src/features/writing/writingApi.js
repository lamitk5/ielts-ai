export async function submitWriting(taskId, responseText) {
  const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
  const response = await fetch('/api/practice/writing/submissions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
    },
    body: JSON.stringify({ taskId, responseText }),
  })
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.error?.message ?? 'Không thể gửi bài viết.')
  return body
}

export async function getWritingSubmissions() {
  const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
  const response = await fetch('/api/practice/writing/submissions', {
    headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {},
  })
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.error?.message ?? 'Không thể tải lịch sử Writing.')
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

export async function startWritingAttempt(taskId) {
  const response = await fetch('/api/practice/writing/attempts', {
    method: 'POST', headers: sessionHeaders(), body: JSON.stringify({ taskId }),
  })
  return readAttemptResponse(response, 'Không thể bắt đầu phiên Writing.')
}

export async function saveWritingAttemptDraft(attemptId, responseText) {
  const response = await fetch(`/api/practice/writing/attempts/${attemptId}/draft`, {
    method: 'PUT', headers: sessionHeaders(), body: JSON.stringify({ responseText }),
  })
  return readAttemptResponse(response, 'Không thể lưu phiên Writing.')
}

export async function submitWritingAttempt(attemptId, responseText) {
  const response = await fetch(`/api/practice/writing/attempts/${attemptId}/submit`, {
    method: 'POST', headers: sessionHeaders(), body: JSON.stringify({ responseText }),
  })
  return readAttemptResponse(response, 'Không thể nộp phiên Writing.')
}
