import { DEFAULT_PREFERENCES } from './preferenceDefaults'
import { CURSOR_COLOR_PRESETS, CURSOR_SIZE_PRESETS } from './cursorAsset'

export const WORKSPACE_RATIO_PRESETS = Object.freeze([40, 50, 60])
export const CURSOR_STYLE_PRESETS = Object.freeze(['default', 'champagne', 'scholar-pen', 'en-feather', 'pixel-scholar'])
export const LOCAL_ONLY_PREFERENCE_KEYS = Object.freeze(['cursorStyle', 'cursorSize', 'cursorColor', 'cursorEffects'])

const allowed = {
  themeMode: ['system', 'light', 'dark'],
  accentPreset: ['gold', 'sapphire', 'emerald', 'burgundy', 'violet', 'slate'],
  fontScale: ['small', 'default', 'large'],
  density: ['spacious', 'default', 'compact'],
  reduceMotion: ['system', 'reduce', 'allow'],
  language: ['vi', 'en'],
  readingSplitRatio: WORKSPACE_RATIO_PRESETS,
  writingSplitRatio: WORKSPACE_RATIO_PRESETS,
  cursorStyle: CURSOR_STYLE_PRESETS,
  cursorSize: CURSOR_SIZE_PRESETS,
  cursorColor: CURSOR_COLOR_PRESETS,
}

const booleanPreferences = ['proactiveAiEnabled', 'crossHighlightEnabled', 'timerDefaultEnabled', 'cursorEffects']

export function isCompletePreferences(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false
  const requiredEnums = Object.entries(allowed).filter(([key]) => key !== 'language' && !LOCAL_ONLY_PREFERENCE_KEYS.includes(key))
  const requiredBooleans = booleanPreferences.filter((key) => !LOCAL_ONLY_PREFERENCE_KEYS.includes(key))
  const localValuesValid = LOCAL_ONLY_PREFERENCE_KEYS.every((key) => (
    value[key] === undefined || (allowed[key] ? allowed[key].includes(value[key]) : typeof value[key] === 'boolean')
  ))
  return requiredEnums.every(([key, choices]) => choices.includes(value[key])) &&
    requiredBooleans.every((key) => typeof value[key] === 'boolean') &&
    localValuesValid
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
  for (const key of ['proactiveAiEnabled', 'crossHighlightEnabled', 'timerDefaultEnabled', 'cursorEffects']) {
    if (typeof input[key] === 'boolean') result[key] = input[key]
  }
  return result
}

export function mergeLocalPreferences(preferences, localSource) {
  const source = localSource && typeof localSource === 'object' ? localSource : {}
  const localValues = Object.fromEntries(LOCAL_ONLY_PREFERENCE_KEYS
    .filter((key) => source[key] !== undefined)
    .map((key) => [key, source[key]]))
  return normalizePreferences({
    ...preferences,
    ...localValues,
  })
}
