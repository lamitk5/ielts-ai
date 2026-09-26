import { useOptionalAuth } from '../../features/auth/AuthProvider'

export function ProtectedAction({ children, fallback = null, onUnauthorized }) {
  const auth = useOptionalAuth()

  if (auth?.isAuthenticated) {
    return typeof children === 'function' ? children() : children
  }

  if (onUnauthorized && typeof onUnauthorized === 'function') {
    onUnauthorized()
  }

  return fallback
}

export default ProtectedAction
