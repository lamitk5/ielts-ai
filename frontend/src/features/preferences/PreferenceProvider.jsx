import { createContext, useContext, useLayoutEffect, useState } from 'react'
import { DEFAULT_PREFERENCES } from './preferenceDefaults'
import { normalizePreferences } from './preferenceSchema'

const PreferenceContext = createContext(null)

const themes = {
  dark: {
    '--background': '#060b16', '--surface': '#0c1424', '--surface-alt': '#101b30',
    '--text': '#f5f7fa', '--text-secondary': '#b7c0cf', '--muted': '#697386',
    '--border': 'rgba(229, 201, 130, 0.16)', '--shadow': '0 16px 40px rgba(0, 0, 0, 0.24)',
  },
  light: {
    '--background': '#f5f7fa', '--surface': '#ffffff', '--surface-alt': '#e8edf4',
    '--text': '#071426', '--text-secondary': '#33445c', '--muted': '#58677a',
    '--border': 'rgba(7, 20, 38, 0.18)', '--shadow': '0 16px 40px rgba(7, 20, 38, 0.1)',
  },
}

const accents = {
  gold: ['#cfae67', '#e5c982', 'rgba(207, 174, 103, 0.16)', '#8a6426'],
  sapphire: ['#5088c6', '#9cc4ee', 'rgba(80, 136, 198, 0.16)', '#255b91'],
  emerald: ['#398f76', '#8ed9bc', 'rgba(57, 143, 118, 0.16)', '#216c56'],
  burgundy: ['#a45169', '#e6a3b4', 'rgba(164, 81, 105, 0.16)', '#8b334d'],
  violet: ['#8468b8', '#c9b2ee', 'rgba(132, 104, 184, 0.16)', '#5f438f'],
  slate: ['#617c98', '#adc2d8', 'rgba(97, 124, 152, 0.16)', '#405972'],
}

const fontScales = { small: '0.9375', default: '1', large: '1.125' }
const densities = { spacious: '1.125', default: '1', compact: '0.875' }

export function applyPreferenceTokens(value) {
  const preferences = normalizePreferences(value)
  const systemDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false
  const systemReduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  const theme = preferences.themeMode === 'system' ? (systemDark ? 'dark' : 'light') : preferences.themeMode
  const reduced = systemReduced || preferences.reduceMotion === 'reduce'
  const [accent, darkStrong, soft, lightStrong] = accents[preferences.accentPreset]
  const strong = theme === 'light' ? lightStrong : darkStrong
  const tokens = {
    ...themes[theme],
    '--accent': accent, '--accent-strong': strong, '--accent-soft': soft,
    '--focus': strong, '--link': strong, '--selected': soft,
    '--border-strong': accent, '--primary-action': accent, '--chart-accent': accent,
    '--success': '#398f76', '--warning': '#cfae67', '--danger': '#a45169', '--info': '#5088c6',
    '--radius': '1rem', '--space': densities[preferences.density],
    '--type-scale': fontScales[preferences.fontScale],
    '--motion-duration': reduced ? '0ms' : '300ms',
  }
  for (const [name, value] of Object.entries(tokens)) document.documentElement.style.setProperty(name, value)
  document.documentElement.style.colorScheme = theme
}

export function PreferenceProvider({ children }) {
  const [preferences, setPreferences] = useState(DEFAULT_PREFERENCES)

  useLayoutEffect(() => {
    applyPreferenceTokens(preferences)
  }, [preferences])

  const updatePreference = (key, value) => {
    setPreferences((current) => normalizePreferences({ ...current, [key]: value }))
  }

  return (
    <PreferenceContext.Provider value={{ preferences, updatePreference }}>
      {children}
    </PreferenceContext.Provider>
  )
}

export function usePreferences() {
  const context = useContext(PreferenceContext)
  if (!context) throw new Error('usePreferences must be used inside PreferenceProvider')
  return context
}
