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
      targetBand: null,
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
    targetBand: typeof raw.targetBand === 'number' ? raw.targetBand : null,
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
    const candidateBand = record?.latestBandEstimate ?? record?.band
    const band = typeof candidateBand === 'number' && !Number.isNaN(candidateBand) ? candidateBand : null
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
    .filter((m) => m && (m.issue || m.description || m.title || m.category))
    .slice(0, 4) // cap at 2-4 high-value mistakes
    .map((m, idx) => ({
      id: m.id || `mistake-${idx}`,
      skill: m.skill || 'general',
      issue: m.issue || m.description || m.title || m.category,
      recommendation: m.recommendation || m.suggestion || null,
      count: typeof m.occurrenceCount === 'number'
        ? m.occurrenceCount
        : typeof m.count === 'number' ? m.count : 1,
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

  const items = Array.isArray(raw.items) ? raw.items.filter(Boolean) : []
  const currentItem = items.find((item) => item.status !== 'COMPLETED' && item.status !== 'SKIPPED') || items[0]
  const nextItem = items.find((item) => item !== currentItem && item.status !== 'COMPLETED' && item.status !== 'SKIPPED')
  const routeFor = (item) => item?.targetRoute || (item?.skill ? `/practice/${String(item.skill).toLowerCase()}` : '/practice/reading')
  const completedItems = items.filter((item) => item.status === 'COMPLETED').length
  const derivedPercent = items.length > 0 ? Math.round((completedItems / items.length) * 100) : 0

  return {
    currentMilestone: raw.currentMilestone || currentItem?.learningObjective || 'Lộ trình đang cập nhật',
    nextMilestone: raw.nextMilestone || nextItem?.learningObjective || null,
    completionPercent: typeof raw.completionPercent === 'number' ? raw.completionPercent : derivedPercent,
    primaryAction: raw.primaryAction ? {
      title: raw.primaryAction.title || 'Tiếp tục luyện tập',
      targetRoute: raw.primaryAction.targetRoute || '/practice/reading',
      skill: raw.primaryAction.skill || 'reading',
    } : currentItem ? {
      title: currentItem.learningObjective || 'Tiếp tục luyện tập',
      targetRoute: routeFor(currentItem),
      skill: String(currentItem.skill || 'reading').toLowerCase(),
    } : null,
    actions: Array.isArray(raw.actions) ? raw.actions : [],
    hasRoadmap: Boolean(raw.currentMilestone || raw.primaryAction || items.length > 0),
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
