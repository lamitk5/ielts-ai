export const MEANINGFUL_EVENT_TYPES = [
  'PRACTICE_COMPLETED',
  'WRITING_SUBMITTED',
  'SPEAKING_SAVED',
  'ASSESSMENT_COMPLETED',
  'EXERCISE_COMPLETED',
]

export function isMeaningfulLearningEvent(event) {
  if (!event || typeof event !== 'object') return false
  const type = event.type
  if (typeof type !== 'string') return false
  return MEANINGFUL_EVENT_TYPES.includes(type.toUpperCase())
}

export function getEventDateKey(timestamp, timezone = 'UTC') {
  if (!timestamp) return null
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return null

  try {
    const formatter = new Intl.DateTimeFormat('en-CA', {
      timeZone: timezone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    })
    return formatter.format(date) // returns YYYY-MM-DD
  } catch {
    return date.toISOString().split('T')[0]
  }
}

export function calculateMeaningfulStreak(events = [], timezone = 'UTC', referenceDate = new Date()) {
  if (!Array.isArray(events)) {
    return { streakDays: 0, isActiveToday: false, activeDates: [] }
  }

  const activeDatesSet = new Set()

  events.forEach((event) => {
    if (isMeaningfulLearningEvent(event)) {
      const dateKey = getEventDateKey(event.timestamp || event.createdAt, timezone)
      if (dateKey) {
        activeDatesSet.add(dateKey)
      }
    }
  })

  const todayKey = getEventDateKey(referenceDate, timezone)
  const isActiveToday = activeDatesSet.has(todayKey)

  // Determine starting point for contiguous sequence
  let checkDate = new Date(referenceDate)
  if (!isActiveToday) {
    // Check if practiced yesterday
    checkDate.setDate(checkDate.getDate() - 1)
    const yesterdayKey = getEventDateKey(checkDate, timezone)
    if (!activeDatesSet.has(yesterdayKey)) {
      return {
        streakDays: 0,
        isActiveToday: false,
        activeDates: Array.from(activeDatesSet),
      }
    }
  }

  let streakDays = 0
  while (true) {
    const currentKey = getEventDateKey(checkDate, timezone)
    if (activeDatesSet.has(currentKey)) {
      streakDays += 1
      checkDate.setDate(checkDate.getDate() - 1)
    } else {
      break
    }
  }

  return {
    streakDays,
    isActiveToday,
    activeDates: Array.from(activeDatesSet),
  }
}
