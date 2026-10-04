const LEVELS = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED']
const SKILLS = ['READING', 'LISTENING', 'WRITING', 'SPEAKING']
const STATES = ['NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED']

const levelOptions = [...LEVELS]
const skillOptions = [...SKILLS]

function readToken() {
  try { return JSON.parse(localStorage.getItem('ielts-ai-tutor.session') ?? 'null')?.token } catch { return null }
}

function editableKeys(goal) {
  return {
    selfReportedLevel: goal.selfReportedLevel ?? null,
    targetBand: goal.targetBand ?? null,
    targetExamDate: goal.targetExamDate ?? null,
    perceivedWeakestSkill: goal.perceivedWeakestSkill ?? null,
    dailyStudyMinutes: goal.dailyStudyMinutes ?? null,
    studyDaysPerWeek: goal.studyDaysPerWeek ?? null,
    state: goal.state ?? 'IN_PROGRESS',
    version: goal.version,
  }
}

function normalize(record) {
  if (!record || !Number.isSafeInteger(record.version) || record.version < 0) {
    throw new Error('Invalid onboarding response')
  }
  return {
    userId: record.userId ?? null,
    selfReportedLevel: levelOptions.includes(record.selfReportedLevel) ? record.selfReportedLevel : null,
    targetBand: typeof record.targetBand === 'number' ? record.targetBand : null,
    targetExamDate: record.targetExamDate ?? null,
    perceivedWeakestSkill: skillOptions.includes(record.perceivedWeakestSkill) ? record.perceivedWeakestSkill : null,
    dailyStudyMinutes: Number.isInteger(record.dailyStudyMinutes) ? record.dailyStudyMinutes : null,
    studyDaysPerWeek: Number.isInteger(record.studyDaysPerWeek) ? record.studyDaysPerWeek : null,
    state: STATES.includes(record.state) ? record.state : 'NOT_STARTED',
    source: record.source ?? 'SELF_REPORTED',
    basis: record.basis ?? 'LEARNER_DECLARATION',
    measuredLevel: record.measuredLevel ?? null,
    measuredWeakestSkill: skillOptions.includes(record.measuredWeakestSkill) ? record.measuredWeakestSkill : null,
    evidenceReference: record.evidenceReference ?? null,
    version: record.version,
    isSelfReportOnly: record.source === 'SELF_REPORTED',
    isSkipped: record.state === 'SKIPPED',
  }
}

async function request(method, body) {
  const token = readToken()
  if (!token) throw new Error('AUTH_REQUIRED')
  const response = await fetch('/api/me/onboarding', {
    method,
    headers: { Authorization: `Bearer ${token}`, ...(body ? { 'Content-Type': 'application/json' } : {}) },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })
  if (response.status === 409) {
    const error = new Error('CONFLICT')
    error.code = 'CONFLICT'
    throw error
  }
  if (!response.ok) throw new Error('Không thể lưu mục tiêu học tập.')
  return normalize(await response.json())
}

export async function getOnboarding() {
  return request('GET')
}

export async function saveOnboarding(goal) {
  return request('PUT', editableKeys(goal))
}

export async function completeOnboarding(state, version) {
  const token = readToken()
  if (!token) throw new Error('AUTH_REQUIRED')
  const response = await fetch('/api/me/onboarding/complete', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ state, version }),
  })
  if (response.status === 409) {
    const error = new Error('CONFLICT')
    error.code = 'CONFLICT'
    throw error
  }
  if (!response.ok) throw new Error('Không thể hoàn tất thiết lập.')
  return normalize(await response.json())
}

export const onboardingOptions = { levels: levelOptions, skills: skillOptions, states: STATES }
