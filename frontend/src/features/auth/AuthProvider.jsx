import { createContext, useContext, useMemo, useState } from 'react'
import { clearStoredSession, getStoredSession, login, logout, register } from '../../services/authApi'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => getStoredSession())

  const value = useMemo(() => ({
    session,
    user: session?.user ?? null,
    isAuthenticated: Boolean(session?.token && session?.user),
    login: async (credentials) => {
      const nextSession = await login(credentials)
      setSession(nextSession)
      return nextSession
    },
    register: async (details) => {
      const nextSession = await register(details)
      setSession(nextSession)
      return nextSession
    },
    logout: async () => {
      await logout()
      clearStoredSession()
      setSession(null)
    },
  }), [session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
