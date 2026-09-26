import { Link, useLocation } from 'react-router-dom'
import { useOptionalAuth } from '../../features/auth/AuthProvider'

export function sanitizeReturnTo(returnTo, fallback = '/') {
  if (
    typeof returnTo === 'string' &&
    returnTo.startsWith('/') &&
    !returnTo.startsWith('//') &&
    !returnTo.includes('://')
  ) {
    return returnTo
  }
  return fallback
}

export function AuthGate({ children, reason, returnTo, forceGate = false }) {
  const auth = useOptionalAuth()
  const location = useLocation()

  if (auth?.isAuthenticated && !forceGate) {
    return children ? <>{children}</> : null
  }

  const currentPath = location ? `${location.pathname}${location.search}${location.hash}` : '/'
  const safeReturn = sanitizeReturnTo(returnTo, currentPath)

  return (
    <div className="auth-gate-card">
      <div className="auth-gate-content">
        <p className="auth-gate-kicker">TÀI KHOẢN HỌC VIÊN</p>
        <h3 className="auth-gate-title font-display">Tham gia cùng IELTS AI Tutor</h3>
        <p className="auth-gate-reason">
          {reason || 'Cần tài khoản thành viên để nhận hỗ trợ cá nhân hóa theo bài học và lưu lại tiến độ.'}
        </p>
        <div className="auth-gate-actions">
          <Link
            to="/login"
            state={{ from: safeReturn }}
            className="button button-primary button-sm auth-gate-login-btn"
          >
            Đăng nhập
          </Link>
          <Link
            to="/register"
            state={{ from: safeReturn }}
            className="button button-outline button-sm auth-gate-register-btn"
          >
            Tạo tài khoản
          </Link>
        </div>
      </div>
    </div>
  )
}

export default AuthGate
