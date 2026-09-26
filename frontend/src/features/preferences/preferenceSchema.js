import { DEFAULT_PREFERENCES } from './preferenceDefaults'

export const WORKSPACE_RATIO_PRESETS = Object.freeze([40, 50, 60])

const allowed = {
  themeMode: ['system', 'light', 'dark'],
  accentPreset: ['gold', 'sapphire', 'emerald', 'burgundy', 'violet', 'slate'],
  fontScale: ['small', 'default', 'large'],
  density: ['spacious', 'default', 'compact'],
  reduceMotion: ['system', 'reduce', 'allow'],
  readingSplitRatio: WORKSPACE_RATIO_PRESETS,
  writingSplitRatio: WORKSPACE_RATIO_PRESETS,
}

const booleanPreferences = ['proactiveAiEnabled', 'crossHighlightEnabled', 'timerDefaultEnabled']

export function isCompletePreferences(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false
  return Object.entries(allowed).every(([key, choices]) => choices.includes(value[key])) &&
    booleanPreferences.every((key) => typeof value[key] === 'boolean')
}

export function isValidPreferenceRecord(record) {
  if (!record || record.version !== 1 || !record.preferences || typeof record.preferences !== 'object' || Array.isArray(record.preferences)) {
    return false
  }
  for (const [key, val] of Object.entries(record.preferences)) {
    if (allowed[key] && !allowed[key].includes(val)) return false
    if (booleanPreferences.includes(key) && typeof val !== 'boolean') {
      return false
    }
  }
  return true
}

export function normalizePreferences(value) {
  const input = value && typeof value === 'object' && !Array.isArray(value) ? value : {}
  const result = { ...DEFAULT_PREFERENCES }
  for (const [key, choices] of Object.entries(allowed)) {
    if (choices.includes(input[key])) result[key] = input[key]
  }
  for (const key of ['proactiveAiEnabled', 'crossHighlightEnabled', 'timerDefaultEnabled']) {
    if (typeof input[key] === 'boolean') result[key] = input[key]
  }
  return result
}
