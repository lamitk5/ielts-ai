import { createContext, useContext, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { useReducedMotion } from 'framer-motion'
import { useOptionalAuth } from '../auth/AuthProvider'
import { DEFAULT_PREFERENCES } from './preferenceDefaults'
import { normalizePreferences } from './preferenceSchema'
import { readAccountPreferenceCache, readGuestPreferences, writeAccountPreferenceCache, writeGuestPreferences } from './preferenceStorage'
import { getPreferences, savePreferences } from '../../services/preferencesApi'

const PreferenceContext = createContext(null)

const themes = {
  dark: {
    '--background': '#060b16', '--surface': '#0c1424', '--surface-alt': '#101b30',
    '--text': '#f5f7fa', '--text-secondary': '#b7c0cf', '--muted': '#929cad',
    '--border': 'rgba(229, 201, 130, 0.16)', '--shadow': '0 16px 40px rgba(0, 0, 0, 0.24)',
  },
  light: {
    '--background': '#f5f7fa', '--surface': '#ffffff', '--surface-alt': '#e8edf4',
    '--text': '#071426', '--text-secondary': '#33445c', '--muted': '#58677a',
    '--border': 'rgba(7, 20, 38, 0.18)', '--shadow': '0 16px 40px rgba(7, 20, 38, 0.1)',
  },
}

const accents = {
  gold: ['#e5c982', 'rgba(207, 174, 103, 0.16)', '#8a6426'],
  sapphire: ['#9cc4ee', 'rgba(80, 136, 198, 0.16)', '#255b91'],
  emerald: ['#8ed9bc', 'rgba(57, 143, 118, 0.16)', '#216c56'],
  burgundy: ['#e6a3b4', 'rgba(164, 81, 105, 0.16)', '#8b334d'],
  violet: ['#c9b2ee', 'rgba(132, 104, 184, 0.16)', '#5f438f'],
  slate: ['#adc2d8', 'rgba(97, 124, 152, 0.16)', '#405972'],
}

const fontScales = { small: '0.9375', default: '1', large: '1.125' }
const densities = { spacious: '1.125', default: '1', compact: '0.875' }

export function applyPreferenceTokens(value) {
  const preferences = normalizePreferences(value)
  const systemDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false
  const systemReduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  const theme = preferences.themeMode === 'system' ? (systemDark ? 'dark' : 'light') : preferences.themeMode
  const reduced = systemReduced || preferences.reduceMotion === 'reduce'
  const [darkStrong, soft, lightStrong] = accents[preferences.accentPreset]
  const strong = theme === 'light' ? lightStrong : darkStrong
  const status = theme === 'light'
    ? { '--success': '#216c56', '--warning': '#8a6426', '--danger': '#8b334d', '--info': '#255b91' }
    : { '--success': '#8ed9bc', '--warning': '#e5c982', '--danger': '#e6a3b4', '--info': '#9cc4ee' }
  const tokens = {
    ...themes[theme],
    ...status,
    '--accent': strong, '--accent-strong': strong, '--accent-soft': soft,
    '--focus': strong, '--link': strong, '--selected': strong, '--selected-soft': soft,
    '--border-strong': strong, '--selected-border': strong,
    '--primary-action': strong, '--primary-action-text': theme === 'light' ? '#ffffff' : '#071426',
    '--selected-text': theme === 'light' ? '#ffffff' : '#071426',
    '--chart-accent': strong,
    '--radius': '1rem', '--space': densities[preferences.density],
    '--type-scale': fontScales[preferences.fontScale],
    '--motion-duration': reduced ? '0ms' : '300ms',
  }
  for (const [name, value] of Object.entries(tokens)) document.documentElement.style.setProperty(name, value)
  document.documentElement.style.colorScheme = theme
  document.documentElement.dataset.reducedMotion = String(reduced)
}

export function PreferenceProvider({ children }) {
  const auth = useOptionalAuth()
  const userId = auth?.isAuthenticated ? auth.user?.id : null
  const initial = () => userId ? (readAccountPreferenceCache(userId)?.preferences ?? DEFAULT_PREFERENCES) : readGuestPreferences()
  const [preferences, setPreferences] = useState(initial)
  const [confirmedPreferences, setConfirmedPreferences] = useState(null)
  const [status, setStatus] = useState(userId ? 'loading' : 'idle')
  const preferencesRef = useRef(preferences)
  const confirmedRef = useRef(null)
  const timerRef = useRef(null)
  const savingEpochRef = useRef(null)
  const epochRef = useRef(0)
  const editRef = useRef(0)
  const dirtyRef = useRef(false)
  const identityRef = useRef(userId)
  const mountedRef = useRef(false)
  identityRef.current = userId
  const [systemDark, setSystemDark] = useState(() => window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false)
  const [systemReduced, setSystemReduced] = useState(() => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false)

  useEffect(() => {
    mountedRef.current = true
    const epoch = ++epochRef.current
    savingEpochRef.current = null
    clearTimeout(timerRef.current)
    editRef.current = 0
    dirtyRef.current = false
    const cached = userId ? readAccountPreferenceCache(userId)?.preferences ?? DEFAULT_PREFERENCES : readGuestPreferences()
    preferencesRef.current = cached
    setPreferences(cached)
    confirmedRef.current = null
    setConfirmedPreferences(null)
    setStatus(userId ? 'loading' : 'idle')
    const current = () => mountedRef.current && epochRef.current === epoch && identityRef.current === userId
    let automaticHydrationRetries = 0
    const hydrate = async (editAtRequest) => {
      try {
        const record = await getPreferences()
        if (!current()) return
        const normalized = normalizePreferences(record)
        confirmedRef.current = record
        setConfirmedPreferences(normalized)
        writeAccountPreferenceCache(userId, normalized, record.version)
        if (!dirtyRef.current && editRef.current === editAtRequest) {
          preferencesRef.current = normalized
          setPreferences(normalized)
          setStatus('synced')
        } else {
          setStatus('saving')
          clearTimeout(timerRef.current)
          timerRef.current = setTimeout(() => persist(preferencesRef.current, editRef.current, epoch), 300)
        }
      } catch {
        if (!current()) return
        setStatus('unsynced')
        if ((dirtyRef.current || editRef.current !== editAtRequest) && automaticHydrationRetries < 1) {
          automaticHydrationRetries += 1
          clearTimeout(timerRef.current)
          timerRef.current = setTimeout(() => hydrate(editRef.current), 300)
        }
      }
    }
    if (userId) void hydrate(editRef.current)
    return () => { mountedRef.current = false; ++epochRef.current; clearTimeout(timerRef.current) }
  }, [userId])

  useLayoutEffect(() => {
    const colorQuery = window.matchMedia?.('(prefers-color-scheme: dark)')
    const motionQuery = window.matchMedia?.('(prefers-reduced-motion: reduce)')
    const onColorChange = (event) => setSystemDark(event.matches)
    const onMotionChange = (event) => setSystemReduced(event.matches)
    colorQuery?.addEventListener?.('change', onColorChange)
    motionQuery?.addEventListener?.('change', onMotionChange)
    return () => {
      colorQuery?.removeEventListener?.('change', onColorChange)
      motionQuery?.removeEventListener?.('change', onMotionChange)
    }
  }, [])

  useLayoutEffect(() => {
    applyPreferenceTokens(preferences)
  }, [preferences, systemDark, systemReduced])

  const persist = async (snapshot, edit, epoch) => {
    const confirmed = confirmedRef.current
    if (!confirmed || epochRef.current !== epoch || identityRef.current !== userId || !mountedRef.current || savingEpochRef.current === epoch) return
    savingEpochRef.current = epoch
    setStatus('saving')
    try {
      const record = await savePreferences(snapshot, confirmed.version)
      if (epochRef.current !== epoch || identityRef.current !== userId || !mountedRef.current) return
      confirmedRef.current = record
      const normalized = normalizePreferences(record)
      setConfirmedPreferences(normalized)
      writeAccountPreferenceCache(userId, normalized, record.version)
      if (editRef.current === edit) {
        dirtyRef.current = false
        preferencesRef.current = normalized
        setPreferences(normalized)
        setStatus('synced')
      } else {
        timerRef.current = setTimeout(() => persist(preferencesRef.current, editRef.current, epoch), 300)
      }
    } catch (error) {
      if (epochRef.current !== epoch) return
      if (error.code === 'CONFLICT') {
        const editAtConflictFetch = editRef.current
        try {
          const record = await getPreferences()
          if (epochRef.current !== epoch || identityRef.current !== userId || !mountedRef.current) return
          const normalized = normalizePreferences(record)
          confirmedRef.current = record
          setConfirmedPreferences(normalized)
          writeAccountPreferenceCache(userId, normalized, record.version)
          if (editRef.current === editAtConflictFetch) {
            preferencesRef.current = normalized
            setPreferences(normalized)
            dirtyRef.current = false
            setStatus('conflict')
          } else {
            setStatus('saving')
          }
        } catch {
          if (epochRef.current === epoch) setStatus('unsynced')
        }
      } else setStatus('unsynced')
    } finally {
      if (savingEpochRef.current === epoch) savingEpochRef.current = null
      if (epochRef.current === epoch && identityRef.current === userId && mountedRef.current && editRef.current !== edit) {
        clearTimeout(timerRef.current)
        timerRef.current = setTimeout(() => persist(preferencesRef.current, editRef.current, epoch), 300)
      }
    }
  }

  const updatePreference = (key, value) => {
    const next = normalizePreferences({ ...preferencesRef.current, [key]: value })
    preferencesRef.current = next
    if (userId) dirtyRef.current = true
    setPreferences(next)
    if (!userId) {
      setStatus(writeGuestPreferences(next) ? 'synced' : 'unsynced')
    } else {
      const edit = ++editRef.current
      clearTimeout(timerRef.current)
      setStatus(confirmedRef.current ? 'saving' : 'loading')
      timerRef.current = setTimeout(() => persist(next, edit, epochRef.current), 300)
    }
  }

  const retry = () => {
    if (!userId) { setStatus(writeGuestPreferences(preferencesRef.current) ? 'synced' : 'unsynced'); return }
    if (confirmedRef.current) persist(preferencesRef.current, ++editRef.current, epochRef.current)
    else {
      const initiatingUserId = userId
      const epoch = epochRef.current
      const editAtRequest = editRef.current
      const isCurrent = () => mountedRef.current && identityRef.current === initiatingUserId && epochRef.current === epoch
      setStatus('loading')
      getPreferences().then((record) => {
        if (!isCurrent()) return
        const normalized = normalizePreferences(record)
        confirmedRef.current = record
        setConfirmedPreferences(normalized)
        writeAccountPreferenceCache(userId, normalized, record.version)
        if (!dirtyRef.current && editRef.current === editAtRequest) {
          preferencesRef.current = normalized
          setPreferences(normalized)
          dirtyRef.current = false
          setStatus('synced')
        } else {
          timerRef.current = setTimeout(() => persist(preferencesRef.current, editRef.current, epoch), 300)
        }
      }).catch(() => { if (isCurrent()) setStatus('unsynced') })
    }
  }

  return (
    <PreferenceContext.Provider value={{ preferences, updatePreference, status, confirmedPreferences, retry, reducedMotion: systemReduced || preferences.reduceMotion === 'reduce' }}>
      {children}
    </PreferenceContext.Provider>
  )
}

export function usePreferences() {
  const context = useContext(PreferenceContext)
  if (!context) throw new Error('usePreferences must be used inside PreferenceProvider')
  return context
}

export function useEffectiveReducedMotion() {
  const context = useContext(PreferenceContext)
  const systemReduced = useReducedMotion()
  return context ? context.reducedMotion : Boolean(systemReduced)
}
