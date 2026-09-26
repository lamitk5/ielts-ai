import { act, render, screen } from '@testing-library/react'
import { describe, expect, test } from 'vitest'
import { PreferenceProvider, applyPreferenceTokens, usePreferences } from '../features/preferences/PreferenceProvider'
import { DEFAULT_PREFERENCES } from '../features/preferences/preferenceDefaults'
import { normalizePreferences } from '../features/preferences/preferenceSchema'
import { PREFERENCE_STORAGE_KEY } from '../features/preferences/preferenceStorage'

function PreferenceProbe() {
  const { preferences, updatePreference } = usePreferences()
  return (
    <button onClick={() => updatePreference('accentPreset', 'emerald')}>
      {preferences.accentPreset}
    </button>
  )
}

describe('preference foundation', () => {
  test('provides safe presentation and learning defaults in a versioned contract', () => {
    expect(PREFERENCE_STORAGE_KEY).toBe('ielts-ai-tutor.preferences.v1')
    expect(DEFAULT_PREFERENCES).toEqual({
      themeMode: 'system',
      accentPreset: 'gold',
      fontScale: 'default',
      density: 'default',
      reduceMotion: 'system',
      proactiveAiEnabled: false,
      crossHighlightEnabled: true,
      timerDefaultEnabled: false,
      readingSplitRatio: 40,
      writingSplitRatio: 40,
    })
  })

  test('normalizes allowlisted choices and strips unrelated data', () => {
    expect(normalizePreferences({
      themeMode: 'light', accentPreset: 'sapphire', fontScale: 'large',
      density: 'compact', reduceMotion: 'reduce', readingSplitRatio: 50,
      writingSplitRatio: 60, proactiveAiEnabled: true, secret: 'discard',
    })).toEqual({
      ...DEFAULT_PREFERENCES,
      themeMode: 'light', accentPreset: 'sapphire', fontScale: 'large',
      density: 'compact', reduceMotion: 'reduce', readingSplitRatio: 50,
      writingSplitRatio: 60, proactiveAiEnabled: true,
    })
  })

  test('rejects invalid enums, ratios, and non-boolean flags', () => {
    expect(normalizePreferences({
      themeMode: 'sepia', accentPreset: 'red', fontScale: 'huge',
      density: 'tiny', reduceMotion: 'sometimes', readingSplitRatio: 41,
      writingSplitRatio: '60', proactiveAiEnabled: 'true',
      crossHighlightEnabled: null, timerDefaultEnabled: 1,
    })).toEqual(DEFAULT_PREFERENCES)
  })

  test('accepts every documented choice', () => {
    const choices = {
      themeMode: ['system', 'light', 'dark'],
      accentPreset: ['gold', 'sapphire', 'emerald', 'burgundy', 'violet', 'slate'],
      fontScale: ['small', 'default', 'large'],
      density: ['spacious', 'default', 'compact'],
      reduceMotion: ['system', 'reduce', 'allow'],
      readingSplitRatio: [40, 50, 60],
      writingSplitRatio: [40, 50, 60],
    }
    for (const [key, values] of Object.entries(choices)) {
      for (const value of values) expect(normalizePreferences({ [key]: value })[key]).toBe(value)
    }
  })

  test('uses a readable focus token on the light surface', () => {
    applyPreferenceTokens({ themeMode: 'light', accentPreset: 'gold' })
    expect(document.documentElement.style.getPropertyValue('--focus')).toBe('#8a6426')
  })

  test('previews a changed preference immediately and applies semantic tokens', () => {
    render(<PreferenceProvider><PreferenceProbe /></PreferenceProvider>)
    const before = document.documentElement.style.getPropertyValue('--accent')
    act(() => screen.getByRole('button', { name: 'gold' }).click())
    expect(screen.getByRole('button', { name: 'emerald' })).toBeInTheDocument()
    expect(document.documentElement.style.getPropertyValue('--accent')).not.toBe(before)
    expect(document.documentElement.style.getPropertyValue('--focus')).toBeTruthy()
    expect(document.documentElement.style.getPropertyValue('--chart-accent')).toBeTruthy()
  })
})
