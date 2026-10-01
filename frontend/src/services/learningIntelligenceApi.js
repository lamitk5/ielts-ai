function getAuthHeaders() {
  try {
    const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
    return session?.token ? { Authorization: `Bearer ${session.token}` } : {}
  } catch {
    return {}
  }
}

async function fetchLearningJson(path, signal) {
  const response = await fetch(`/api${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeaders(),
    },
    signal,
  })

  if (!response.ok) {
    if (response.status === 404) {
      return null
    }
    throw new Error(`Lỗi kết nối học tập (${response.status})`)
  }

  return response.json()
}

export async function getLearningProfile(signal) {
  return fetchLearningJson('/learning/profile', signal)
}

export async function getLearningSkills(signal) {
  return fetchLearningJson('/learning/skills', signal)
}

export async function getLearningMistakes(signal) {
  return fetchLearningJson('/learning/mistakes', signal)
}

export async function getLearningRoadmap(signal) {
  return fetchLearningJson('/learning/roadmap', signal)
}

export async function getLearningActivity(signal) {
  return fetchLearningJson('/learning/activity', signal)
}

export async function getLearningDashboardData(signal) {
  const [profileRes, skillsRes, mistakesRes, roadmapRes, activityRes] =
    await Promise.allSettled([
      getLearningProfile(signal),
      getLearningSkills(signal),
      getLearningMistakes(signal),
      getLearningRoadmap(signal),
      getLearningActivity(signal),
    ])

  const profile = profileRes.status === 'fulfilled' ? profileRes.value : null
  const skills = skillsRes.status === 'fulfilled' ? skillsRes.value : null
  const mistakes = mistakesRes.status === 'fulfilled' ? mistakesRes.value : null
  const roadmap = roadmapRes.status === 'fulfilled' ? roadmapRes.value : null
  const activity = activityRes.status === 'fulfilled' ? activityRes.value : null

  const hasAnyData = Boolean(profile || (skills && skills.length > 0) || (mistakes && mistakes.length > 0) || roadmap || (activity && activity.length > 0))

  if (!hasAnyData && (profileRes.status === 'rejected' || skillsRes.status === 'rejected')) {
    const firstErr = (profileRes.status === 'rejected' ? profileRes.reason : skillsRes.reason)
    if (firstErr?.name !== 'AbortError') {
      throw firstErr instanceof Error ? firstErr : new Error('Không thể tải dữ liệu học tập')
    }
  }

  return {
    profile,
    skills,
    mistakes,
    roadmap,
    activity,
    hasData: hasAnyData,
  }
}
