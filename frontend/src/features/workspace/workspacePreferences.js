import { WORKSPACE_RATIO_PRESETS } from '../preferences/preferenceSchema'

const preferenceKeys = Object.freeze({
  reading: 'readingSplitRatio',
  writing: 'writingSplitRatio',
})

export function getWorkspacePreferenceKey(workspace) {
  const key = preferenceKeys[workspace]
  if (!key) throw new Error(`Unsupported learning workspace: ${workspace}`)
  return key
}

export function getWorkspaceRatio(preferences, workspace) {
  return preferences?.[getWorkspacePreferenceKey(workspace)] ?? 40
}

export function saveWorkspaceRatio(updatePreference, workspace, ratio) {
  if (!WORKSPACE_RATIO_PRESETS.includes(ratio)) return
  updatePreference(getWorkspacePreferenceKey(workspace), ratio)
}
