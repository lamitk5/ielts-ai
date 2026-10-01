import { Navigate, useLocation } from 'react-router-dom'
import { useOptionalAuth } from './AuthProvider'

function RequireAdminRoute({ children, allowLegacyToken = false }) {
  const auth = useOptionalAuth()
  const location = useLocation()

  if (!auth?.isAuthenticated && !allowLegacyToken) {
    return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}${location.hash}` }} />
  }

  if (auth?.isAuthenticated && String(auth.user?.role ?? '').toUpperCase() !== 'ADMIN') {
    return <Navigate to="/" replace />
  }

  return children
}

export default RequireAdminRoute
