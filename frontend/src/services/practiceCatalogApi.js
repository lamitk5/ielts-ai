const SKILLS = ['reading', 'listening', 'writing', 'speaking']

function headers() {
  try {
    const session = JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')
    return session?.token ? { Authorization: `Bearer ${session.token}` } : {}
  } catch { return {} }
}

async function request(url) {
  const response = await fetch(url, { headers: headers() })
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.error?.message ?? 'Không thể tải bài luyện tập.')
  return body
}

export async function listPracticeCatalog() {
  const results = await Promise.all(SKILLS.map(async (skill) => {
    try {
      const sets = await request(`/api/practice/${skill}/sets`)
      return (Array.isArray(sets) ? sets : []).filter((set) => set?.active !== false).map((set) => ({
        ...set,
        id: set.id ?? set.setId,
        skill: String(set.skill ?? skill).toLowerCase(),
        title: set.title ?? set.name ?? skill,
      }))
    } catch { return [] }
  }))
  return results.flat()
}

export function getPracticeSet(skill, setId) {
  return request(`/api/practice/${encodeURIComponent(skill)}/sets/${encodeURIComponent(setId)}`)
}
