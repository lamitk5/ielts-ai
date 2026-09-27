import { act, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, test, vi } from 'vitest'
import { PreferenceProvider, applyPreferenceTokens, usePreferences } from '../features/preferences/PreferenceProvider'
import { DEFAULT_PREFERENCES } from '../features/preferences/preferenceDefaults'
import { normalizePreferences } from '../features/preferences/preferenceSchema'
import { PREFERENCE_STORAGE_KEY } from '../features/preferences/preferenceStorage'
import AnimatedSection from '../components/common/AnimatedSection'
import ParallaxLayer from '../components/motion/ParallaxLayer'
import '../styles/globals.css'

afterEach(() => vi.unstubAllGlobals())

function luminance(hex) {
  const rgb = hex.match(/[a-f\d]{2}/gi).map((part) => parseInt(part, 16) / 255)
  const linear = rgb.map((channel) => channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4)
  return linear[0] * 0.2126 + linear[1] * 0.7152 + linear[2] * 0.0722
}

function contrast(first, second) {
  const values = [luminance(first), luminance(second)].sort((a, b) => b - a)
  return (values[0] + 0.05) / (values[1] + 0.05)
}

function token(name) {
  return document.documentElement.style.getPropertyValue(name)
}

function PreferenceProbe() {
  const { preferences, updatePreference } = usePreferences()
  return (
    <button onClick={() => updatePreference('accentPreset', 'emerald')}>
      {preferences.accentPreset}
    </button>
  )
}

function MotionProbe() {
  const { updatePreference } = usePreferences()
  return (
    <>
      <button onClick={() => updatePreference('reduceMotion', 'reduce')}>Reduce motion</button>
      <AnimatedSection data-testid="animated-section">Readable section</AnimatedSection>
      <ParallaxLayer>Static decoration</ParallaxLayer>
    </>
  )
}

function mediaQuery(initial) {
  const listeners = new Set()
  return {
    matches: initial,
    addEventListener: (_type, listener) => listeners.add(listener),
    removeEventListener: (_type, listener) => listeners.delete(listener),
    change(matches) {
      this.matches = matches
      for (const listener of listeners) listener({ matches })
    },
  }
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
      language: 'vi',
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
      language: ['vi', 'en'],
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

  test('keeps text, control boundaries, action fills, and chart marks readable for every preset in both themes', () => {
    for (const themeMode of ['light', 'dark']) {
      for (const accentPreset of ['gold', 'sapphire', 'emerald', 'burgundy', 'violet', 'slate']) {
        applyPreferenceTokens({ themeMode, accentPreset })
        for (const surfaceRole of ['--surface', '--background', '--surface-alt']) {
          const surface = token(surfaceRole)
          for (const role of ['--text', '--text-secondary', '--muted', '--accent', '--accent-strong', '--link', '--success', '--warning', '--danger', '--info']) {
            expect(token(role), `${themeMode}/${accentPreset}/${role}`).toMatch(/^#[0-9a-f]{6}$/)
            expect(contrast(token(role), surface), `${themeMode}/${accentPreset}/${surfaceRole}/${role}`).toBeGreaterThanOrEqual(4.5)
          }
          for (const role of ['--focus', '--border-strong', '--selected', '--selected-border', '--primary-action', '--chart-accent']) {
            expect(token(role), `${themeMode}/${accentPreset}/${role}`).toMatch(/^#[0-9a-f]{6}$/)
            expect(contrast(token(role), surface), `${themeMode}/${accentPreset}/${surfaceRole}/${role}`).toBeGreaterThanOrEqual(3)
          }
        }
        expect(contrast(token('--primary-action-text'), token('--primary-action')), `${themeMode}/${accentPreset}/action text`).toBeGreaterThanOrEqual(4.5)
        expect(token('--selected-text')).toMatch(/^#[0-9a-f]{6}$/)
        expect(contrast(token('--selected-text'), token('--selected')), `${themeMode}/${accentPreset}/selected text`).toBeGreaterThanOrEqual(4.5)
      }
    }
  })

  test('publishes the canonical Academic Luxury semantic roles for every theme and accent', () => {
    const roles = [
      '--bg-page', '--bg-section', '--surface-1', '--surface-2', '--surface-elevated',
      '--surface-interactive', '--text-primary', '--text-secondary', '--text-muted',
      '--text-inverse', '--border-subtle', '--border-strong', '--accent', '--accent-hover',
      '--accent-soft', '--focus-ring', '--success', '--warning', '--danger',
      '--shadow-sm', '--shadow-md', '--shadow-lg', '--overlay', '--glass-bg',
    ]
    for (const themeMode of ['light', 'dark']) {
      for (const accentPreset of ['gold', 'sapphire', 'emerald', 'burgundy', 'violet', 'slate']) {
        applyPreferenceTokens({ themeMode, accentPreset })
        roles.forEach((role) => expect(token(role), `${themeMode}/${accentPreset}/${role}`).toBeTruthy())
      }
    }
  })

  test('keeps the unmounted dark CSS defaults readable before the provider starts', () => {
    const names = ['--muted', '--border-strong']
    const previous = names.map((name) => document.documentElement.style.getPropertyValue(name))
    for (const name of names) document.documentElement.style.removeProperty(name)
    try {
      const styles = getComputedStyle(document.documentElement)
      expect(contrast(styles.getPropertyValue('--muted').trim(), '#0c1424')).toBeGreaterThanOrEqual(4.5)
      expect(contrast(styles.getPropertyValue('--border-strong').trim(), '#0c1424')).toBeGreaterThanOrEqual(3)
    } finally {
      names.forEach((name, index) => document.documentElement.style.setProperty(name, previous[index]))
    }
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

  test('explicit reduce motion stops existing CSS and Framer motion when the OS allows motion', () => {
    vi.stubGlobal('matchMedia', () => mediaQuery(false))
    render(<PreferenceProvider><MotionProbe /></PreferenceProvider>)
    expect(document.documentElement).toHaveAttribute('data-reduced-motion', 'false')
    act(() => screen.getByRole('button', { name: 'Reduce motion' }).click())
    expect(document.documentElement).toHaveAttribute('data-reduced-motion', 'true')
    expect(token('--motion-duration')).toBe('0ms')
    expect(screen.getByTestId('animated-section')).not.toHaveStyle({ opacity: '0' })
    expect(screen.getByText('Static decoration').closest('.parallax-layer')).not.toHaveAttribute('style')
  })

  test('OS reduced motion still wins over an explicit allow choice', () => {
    vi.stubGlobal('matchMedia', (query) => mediaQuery(query.includes('reduced-motion')))
    applyPreferenceTokens({ reduceMotion: 'allow' })
    expect(token('--motion-duration')).toBe('0ms')
    expect(document.documentElement).toHaveAttribute('data-reduced-motion', 'true')
  })

  test('system theme and motion changes update tokens without a preference edit', () => {
    const color = mediaQuery(false)
    const motion = mediaQuery(false)
    vi.stubGlobal('matchMedia', (query) => query.includes('color-scheme') ? color : motion)
    const { unmount } = render(<PreferenceProvider><span>Preferences</span></PreferenceProvider>)
    expect(token('--background')).toBe('#f5f7fa')
    act(() => color.change(true))
    expect(token('--background')).toBe('#060b16')
    act(() => motion.change(true))
    expect(token('--motion-duration')).toBe('0ms')
    expect(document.documentElement).toHaveAttribute('data-reduced-motion', 'true')
    unmount()
    act(() => color.change(false))
    expect(token('--background')).toBe('#060b16')
  })
})
