import { createContext, useContext, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { useReducedMotion } from 'framer-motion'
import { useOptionalAuth } from '../auth/AuthProvider'
import { getStoredSession } from '../../services/authApi'
import { DEFAULT_PREFERENCES } from './preferenceDefaults'
import { LOCAL_ONLY_PREFERENCE_KEYS, mergeLocalPreferences, normalizePreferences } from './preferenceSchema'
import { createCursorAsset, getCursorEffectColor, normalizeCursorSizePercent, resolveCursorColor } from './cursorAsset'
import { readAccountPreferenceCache, readGuestPreferences, writeAccountPreferenceCache, writeGuestPreferences } from './preferenceStorage'
import { getPreferences, savePreferences } from '../../services/preferencesApi'
import { translate as translateUi } from './uiTranslations'

const PreferenceContext = createContext(null)

const themes = {
  dark: {
    '--background': '#060b16', '--surface': '#0c1424', '--surface-alt': '#101b30',
    '--text': '#f5f7fa', '--text-secondary': '#b7c0cf', '--muted': '#929cad',
    '--border': 'rgba(229, 201, 130, 0.16)', '--shadow': '0 16px 40px rgba(0, 0, 0, 0.24)',
    '--surface-glass': 'rgba(12, 20, 36, 0.78)', '--surface-raised': '#17253d',
    '--border-soft': 'rgba(229, 201, 130, 0.16)', '--shadow-soft': '0 12px 30px rgba(0, 0, 0, 0.16)',
    '--navy': '#071426', '--navy-light': '#102746', '--gold': '#cfae67', '--gold-light': '#e5c982',
  },
  light: {
    '--background': '#f3ecdf', '--surface': '#faf5ec', '--surface-alt': '#eee4d3',
    '--text': '#10233f', '--text-secondary': '#364863', '--muted': '#52627a',
    '--border': 'rgba(122, 95, 57, 0.2)', '--shadow': '0 16px 40px rgba(74, 56, 31, 0.12)',
    '--surface-glass': 'rgba(250, 245, 236, 0.82)', '--surface-raised': '#fffaf1',
    '--border-soft': 'rgba(122, 95, 57, 0.2)', '--shadow-soft': '0 12px 30px rgba(74, 56, 31, 0.12)',
    '--navy': '#10233f', '--navy-light': '#233d5c', '--gold': '#a87932', '--gold-light': '#9b6b2c',
  },
}

const accents = {
  gold: ['#e5c982', 'rgba(207, 174, 103, 0.16)', '#7a591f', '#a97832', '#071426'],
  sapphire: ['#9cc4ee', 'rgba(80, 136, 198, 0.16)', '#255b91', '#4679ad', '#ffffff'],
  emerald: ['#8ed9bc', 'rgba(57, 143, 118, 0.16)', '#216c56', '#35765f', '#ffffff'],
  burgundy: ['#e6a3b4', 'rgba(164, 81, 105, 0.16)', '#8b334d', '#a6516d', '#ffffff'],
  violet: ['#c9b2ee', 'rgba(132, 104, 184, 0.16)', '#5f438f', '#7759a9', '#ffffff'],
  slate: ['#adc2d8', 'rgba(97, 124, 152, 0.16)', '#405972', '#5c7894', '#ffffff'],
}

const fontScales = { small: '0.9375', default: '1', large: '1.125' }
const densities = { spacious: '1.125', default: '1', compact: '0.875' }

export function applyPreferenceTokens(value) {
  const preferences = normalizePreferences(value)
  const systemDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false
  const systemReduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  const theme = preferences.themeMode === 'system' ? (systemDark ? 'dark' : 'light') : preferences.themeMode
  const reduced = systemReduced || preferences.reduceMotion === 'reduce'
  const coarsePointer = window.matchMedia?.('(pointer: coarse)').matches ?? false
  const [darkStrong, soft, lightStrong, lightAction, lightActionText] = accents[preferences.accentPreset]
  const strong = theme === 'light' ? lightStrong : darkStrong
  const cursorColor = resolveCursorColor(preferences.cursorColor, strong)
  const status = theme === 'light'
    ? { '--success': '#216c56', '--warning': '#7a591f', '--danger': '#8b334d', '--info': '#255b91' }
    : { '--success': '#8ed9bc', '--warning': '#e5c982', '--danger': '#e6a3b4', '--info': '#9cc4ee' }
  const tokens = {
    ...themes[theme],
    ...status,
    '--bg-page': themes[theme]['--background'],
    '--bg-section': themes[theme]['--surface-alt'],
    '--surface-1': themes[theme]['--surface'],
    '--surface-2': themes[theme]['--surface-alt'],
    '--surface-elevated': themes[theme]['--surface-raised'],
    '--surface-glass': themes[theme]['--surface-glass'],
    '--surface-raised': themes[theme]['--surface-raised'],
    '--surface-interactive': themes[theme]['--surface-alt'],
    '--text-primary': themes[theme]['--text'],
    '--text-secondary': themes[theme]['--text-secondary'],
    '--text-muted': themes[theme]['--muted'],
    '--text-inverse': theme === 'light' ? '#ffffff' : '#071426',
    '--border-subtle': themes[theme]['--border'],
    '--border-soft': themes[theme]['--border-soft'],
    '--overlay': 'rgba(0, 0, 0, 0.65)',
    '--glass-bg': themes[theme]['--surface-glass'],
    '--shadow-sm': theme === 'light' ? '0 2px 8px rgba(7, 20, 38, 0.08)' : '0 2px 8px rgba(0, 0, 0, 0.18)',
    '--shadow-md': themes[theme]['--shadow'],
    '--shadow-lg': theme === 'light' ? '0 24px 64px rgba(7, 20, 38, 0.14)' : '0 24px 64px rgba(0, 0, 0, 0.32)',
    '--shadow-soft': themes[theme]['--shadow-soft'],
    '--navy': themes[theme]['--navy'], '--navy-light': themes[theme]['--navy-light'],
    '--gold': themes[theme]['--gold'], '--gold-light': themes[theme]['--gold-light'],
    '--accent': strong, '--accent-strong': strong, '--accent-soft': soft,
    '--accent-hover': lightStrong,
    '--focus': strong, '--focus-ring': strong, '--link': strong, '--selected': strong, '--selected-soft': soft,
    '--border-strong': strong, '--selected-border': strong,
    '--primary-action': theme === 'light' ? lightAction : strong,
    '--primary-action-text': theme === 'light' ? lightActionText : '#071426',
    '--selected-text': theme === 'light' ? '#ffffff' : '#071426',
    '--chart-accent': strong,
    '--radius': '1rem', '--space': densities[preferences.density],
    '--type-scale': fontScales[preferences.fontScale],
    '--motion-duration': reduced ? '0ms' : '300ms',
  }
  for (const [name, value] of Object.entries(tokens)) document.documentElement.style.setProperty(name, value)
  document.documentElement.style.colorScheme = theme
  document.documentElement.dataset.theme = theme
  document.documentElement.dataset.density = preferences.density
  document.documentElement.dataset.reducedMotion = String(reduced)
  document.documentElement.dataset.language = preferences.language
  document.documentElement.dataset.cursorStyle = preferences.cursorStyle
  document.documentElement.dataset.cursorSize = String(normalizeCursorSizePercent(preferences.cursorSizePercent))
  document.documentElement.dataset.cursorColor = preferences.cursorColor
  document.documentElement.style.setProperty('--cursor-asset', createCursorAsset({
    style: preferences.cursorStyle,
    sizePercent: preferences.cursorSizePercent,
    color: cursorColor,
  }))
  document.documentElement.style.setProperty('--cursor-effect-color', getCursorEffectColor(cursorColor))
  document.documentElement.dataset.cursorEffects = !coarsePointer && !reduced && preferences.cursorEffects ? 'on' : 'off'
  document.documentElement.dataset.cursorPointer = coarsePointer ? 'coarse' : 'fine'
}

export function PreferenceProvider({ children }) {
  const existing = useContext(PreferenceContext)
  if (existing) return children
  return <PreferenceRootProvider>{children}</PreferenceRootProvider>
}

function PreferenceRootProvider({ children }) {
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
  const isStoredIdentityCurrent = (expectedUserId) => {
    if (!expectedUserId) return !getStoredSession()?.user?.id
    return getStoredSession()?.user?.id === expectedUserId
  }

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
    const current = () => mountedRef.current
      && epochRef.current === epoch
      && identityRef.current === userId
      && isStoredIdentityCurrent(userId)
    let automaticHydrationRetries = 0
    const hydrate = async (editAtRequest) => {
      try {
        const record = await getPreferences()
        if (!current()) return
        const normalized = mergeLocalPreferences(normalizePreferences(record), cached)
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
      const normalized = mergeLocalPreferences(normalizePreferences(record), snapshot)
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
          const normalized = mergeLocalPreferences(normalizePreferences(record), preferencesRef.current)
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
      if (LOCAL_ONLY_PREFERENCE_KEYS.includes(key)) {
        writeAccountPreferenceCache(userId, next, confirmedRef.current?.version)
      }
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
      const isCurrent = () => mountedRef.current
        && identityRef.current === initiatingUserId
        && epochRef.current === epoch
        && isStoredIdentityCurrent(initiatingUserId)
      setStatus('loading')
      getPreferences().then((record) => {
        if (!isCurrent()) return
        const normalized = mergeLocalPreferences(normalizePreferences(record), preferencesRef.current)
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

  const translate = (key, fallback) => translateUi(preferences.language, key, fallback)

  return (
    <PreferenceContext.Provider value={{ preferences, updatePreference, status, confirmedPreferences, retry, translate, reducedMotion: systemReduced || preferences.reduceMotion === 'reduce' }}>
      {children}
    </PreferenceContext.Provider>
  )
}

export function usePreferences() {
  const context = useContext(PreferenceContext)
  if (!context) throw new Error('usePreferences must be used inside PreferenceProvider')
  return context
}

export function useOptionalPreferences() {
  return useContext(PreferenceContext)
}

export function useEffectiveReducedMotion() {
  const context = useContext(PreferenceContext)
  const systemReduced = useReducedMotion()
  return context ? context.reducedMotion : Boolean(systemReduced)
}
