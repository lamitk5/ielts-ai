async function getJson(path) {
  const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
  const response = await fetch(`/api${path}`, {
    headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {},
  })
  if (!response.ok) throw new Error('Không thể tải tiến độ hiện tại.')
  return response.json()
}

export async function getMemberProgress() {
  const [progress, activity] = await Promise.all([getJson('/me/progress'), getJson('/me/activity')])
  return {
    user: null,
    progress: progress.skills.map(({ skill, band }) => ({ skill, band })),
    mistakes: [],
    activity,
  }
}
