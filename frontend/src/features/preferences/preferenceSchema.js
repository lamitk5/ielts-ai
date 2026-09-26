import { DEFAULT_PREFERENCES } from './preferenceDefaults'

const allowed = {
  themeMode: ['system', 'light', 'dark'],
  accentPreset: ['gold', 'sapphire', 'emerald', 'burgundy', 'violet', 'slate'],
  fontScale: ['small', 'default', 'large'],
  density: ['spacious', 'default', 'compact'],
  reduceMotion: ['system', 'reduce', 'allow'],
  readingSplitRatio: [40, 50, 60],
  writingSplitRatio: [40, 50, 60],
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
