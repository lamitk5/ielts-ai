function sessionHeaders() {
  const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
  return {
    'Content-Type': 'application/json',
    ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
  }
}

async function handleResponse(response, fallbackMsg) {
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.error?.message ?? body?.message ?? fallbackMsg)
  return body
}

export async function submitWriting(taskId, responseText) {
  const response = await fetch('/api/practice/writing/submissions', {
    method: 'POST',
    headers: sessionHeaders(),
    body: JSON.stringify({ taskId, responseText }),
  })
  return handleResponse(response, 'Không thể gửi bài viết.')
}

export async function getWritingSubmissions() {
  const response = await fetch('/api/practice/writing/submissions', {
    headers: sessionHeaders(),
  })
  return handleResponse(response, 'Không thể tải lịch sử Writing.')
}

export async function startWritingAttempt(taskId) {
  const response = await fetch('/api/practice/writing/attempts', {
    method: 'POST',
    headers: sessionHeaders(),
    body: JSON.stringify({ taskId }),
  })
  return handleResponse(response, 'Không thể bắt đầu phiên Writing.')
}

export async function saveWritingAttemptDraft(attemptId, responseText) {
  const response = await fetch(`/api/practice/writing/attempts/${attemptId}/draft`, {
    method: 'PUT',
    headers: sessionHeaders(),
    body: JSON.stringify({ responseText }),
  })
  return handleResponse(response, 'Không thể lưu phiên Writing.')
}

export async function submitWritingAttempt(attemptId, responseText) {
  const response = await fetch(`/api/practice/writing/attempts/${attemptId}/submit`, {
    method: 'POST',
    headers: sessionHeaders(),
    body: JSON.stringify({ responseText }),
  })
  return handleResponse(response, 'Không thể nộp phiên Writing.')
}

export async function getWritingVersions(submissionId) {
  const response = await fetch(`/api/practice/writing/submissions/${submissionId}/versions`, {
    headers: sessionHeaders(),
  })
  return handleResponse(response, 'Không thể tải danh sách phiên bản.')
}

export async function createWritingVersion(submissionId, responseText, parentVersionId = null) {
  const response = await fetch(`/api/practice/writing/submissions/${submissionId}/versions`, {
    method: 'POST',
    headers: sessionHeaders(),
    body: JSON.stringify({ responseText, parentVersionId }),
  })
  return handleResponse(response, 'Không thể tạo phiên bản mới.')
}

export async function evaluateWritingVersion(submissionId, versionId) {
  const response = await fetch(`/api/practice/writing/submissions/${submissionId}/versions/${versionId}/evaluate`, {
    method: 'POST',
    headers: sessionHeaders(),
  })
  return handleResponse(response, 'Không thể đánh giá phiên bản.')
}

export async function retryWritingEvaluation(submissionId, versionId) {
  const response = await fetch(`/api/practice/writing/submissions/${submissionId}/versions/${versionId}/retry`, {
    method: 'POST',
    headers: sessionHeaders(),
  })
  return handleResponse(response, 'Không thể thử lại đánh giá.')
}

export async function getWritingEvaluation(submissionId, versionId) {
  const response = await fetch(`/api/practice/writing/submissions/${submissionId}/versions/${versionId}/evaluation`, {
    headers: sessionHeaders(),
  })
  return handleResponse(response, 'Không thể tải kết quả đánh giá.')
}

export async function compareWritingVersions(submissionId, baseVersion, targetVersion) {
  const response = await fetch(
    `/api/practice/writing/submissions/${submissionId}/compare?baseVersion=${baseVersion}&targetVersion=${targetVersion}`,
    { headers: sessionHeaders() }
  )
  return handleResponse(response, 'Không thể so sánh các phiên bản.')
}
