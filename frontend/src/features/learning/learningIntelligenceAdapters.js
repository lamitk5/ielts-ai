const FIRST_CLASS_SKILLS = ['reading', 'listening', 'writing', 'speaking']

const SKILL_NAMES_VI = {
  reading: 'Reading',
  listening: 'Listening',
  writing: 'Writing',
  speaking: 'Speaking',
}

export function normalizeLearningProfile(raw) {
  if (!raw || typeof raw !== 'object') {
    return {
      userId: null,
      estimatedBand: null,
      bandLabel: 'Chưa đủ dữ liệu',
      targetBand: 7.0,
      targetDate: null,
      daysRemaining: null,
      hasSufficientData: false,
    }
  }

  const band = typeof raw.estimatedBand === 'number' && !Number.isNaN(raw.estimatedBand) ? raw.estimatedBand : null

  return {
    userId: raw.userId ?? null,
    estimatedBand: band,
    bandLabel: band !== null ? 'Band ước lượng' : 'Chưa đủ dữ liệu',
    targetBand: typeof raw.targetBand === 'number' ? raw.targetBand : 7.0,
    targetDate: raw.targetDate ?? null,
    daysRemaining: typeof raw.daysRemaining === 'number' ? raw.daysRemaining : null,
    hasSufficientData: band !== null,
  }
}

export function normalizeSkillRecords(raw) {
  const recordsMap = new Map()

  if (Array.isArray(raw)) {
    raw.forEach((item) => {
      if (item && typeof item.skill === 'string') {
        const key = item.skill.toLowerCase()
        if (FIRST_CLASS_SKILLS.includes(key)) {
          recordsMap.set(key, item)
        }
      }
    })
  }

  return FIRST_CLASS_SKILLS.map((skillKey) => {
    const record = recordsMap.get(skillKey)
    const band = typeof record?.band === 'number' && !Number.isNaN(record.band) ? record.band : null
    const hasData = band !== null

    return {
      skill: skillKey,
      name: SKILL_NAMES_VI[skillKey],
      band,
      bandLabel: hasData ? `Band ước lượng ${band.toFixed(1)}` : 'Chưa đủ dữ liệu',
      practiceCount: typeof record?.practiceCount === 'number' ? record.practiceCount : 0,
      hasSufficientData: hasData,
    }
  })
}

export function normalizeMistakes(raw) {
  if (!Array.isArray(raw)) return []

  const valid = raw
    .filter((m) => m && (m.issue || m.description || m.title))
    .slice(0, 4) // cap at 2-4 high-value mistakes
    .map((m, idx) => ({
      id: m.id || `mistake-${idx}`,
      skill: m.skill || 'general',
      issue: m.issue || m.description || m.title,
      recommendation: m.recommendation || m.suggestion || null,
      count: typeof m.count === 'number' ? m.count : 1,
    }))

  return valid
}

export function normalizeRoadmap(raw) {
  if (!raw || typeof raw !== 'object') {
    return {
      currentMilestone: 'Chưa có lộ trình',
      nextMilestone: null,
      completionPercent: 0,
      primaryAction: null,
      actions: [],
      hasRoadmap: false,
    }
  }

  return {
    currentMilestone: raw.currentMilestone || 'Lộ trình đang cập nhật',
    nextMilestone: raw.nextMilestone || null,
    completionPercent: typeof raw.completionPercent === 'number' ? raw.completionPercent : 0,
    primaryAction: raw.primaryAction ? {
      title: raw.primaryAction.title || 'Tiếp tục luyện tập',
      targetRoute: raw.primaryAction.targetRoute || '/practice/reading',
      skill: raw.primaryAction.skill || 'reading',
    } : null,
    actions: Array.isArray(raw.actions) ? raw.actions : [],
    hasRoadmap: Boolean(raw.currentMilestone || raw.primaryAction),
  }
}

export function normalizeLearningActivity(raw) {
  if (!Array.isArray(raw)) return []

  return raw
    .filter((e) => e && e.type && (e.timestamp || e.createdAt))
    .map((e, idx) => ({
      id: e.id || `activity-${idx}`,
      type: e.type,
      skill: e.skill || 'general',
      timestamp: e.timestamp || e.createdAt,
      score: typeof e.score === 'number' ? e.score : null,
    }))
}
