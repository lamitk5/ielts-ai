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
