import { createContext, Fragment, useContext, useMemo, useState } from 'react'
import { clearStoredSession, getStoredSession, login, logout, register } from '../../services/authApi'
import { clearAccountPreferenceCache } from '../preferences/preferenceStorage'
import { clearSessionSnapshot } from '../session/sessionStorage'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => getStoredSession())

  const value = useMemo(() => ({
    session,
    user: session?.user ?? null,
    isAuthenticated: Boolean(session?.token && session?.user),
    login: async (credentials) => {
      const nextSession = await login(credentials)
      if (session?.user?.id && session.user.id !== nextSession.user?.id) {
        clearAccountPreferenceCache(session.user.id)
        clearSessionSnapshot(session.user.id)
      }
      setSession(nextSession)
      return nextSession
    },
    register: async (details) => {
      const nextSession = await register(details)
      if (session?.user?.id && session.user.id !== nextSession.user?.id) {
        clearAccountPreferenceCache(session.user.id)
        clearSessionSnapshot(session.user.id)
      }
      setSession(nextSession)
      return nextSession
    },
    logout: async () => {
      try {
        await logout()
      } finally {
        if (session?.user?.id) {
          clearAccountPreferenceCache(session.user.id)
          clearSessionSnapshot(session.user.id)
        }
        try { clearStoredSession() } catch { /* storage may be unavailable */ }
        setSession(null)
      }
    },
  }), [session])

  return <AuthContext.Provider value={value}><Fragment key={session?.user?.id ?? 'guest'}>{children}</Fragment></AuthContext.Provider>
}

export function useOptionalAuth() {
  return useContext(AuthContext)
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
