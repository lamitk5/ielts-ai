import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import Button from '../components/common/Button'
import AuthCinematicShell from '../components/auth/AuthCinematicShell'
import PasswordFlashlightField from '../components/auth/PasswordFlashlightField'
import { useAuth } from '../features/auth/AuthProvider'
import { useOptionalPreferences } from '../features/preferences/PreferenceProvider'

function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login } = useAuth()
  const preferenceContext = useOptionalPreferences()
  const translate = preferenceContext?.translate ?? ((_key, fallback) => fallback)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event, isLampOn) {
    event.preventDefault()
    if (!isLampOn) return
    setError('')
    setIsSubmitting(true)
    try {
      await login({ email, password })
      navigate(location.state?.from ?? '/', { replace: true })
    } catch (submissionError) {
      setError(submissionError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthCinematicShell labelledBy="login-title">
      {({ isLampOn }) => <div className="auth-card">
        <p className="eyebrow">LUMEN IELTS · AI TUTOR</p>
        <h1 id="login-title" className="font-display">{translate('login', 'Đăng nhập')}</h1>
        <p className="foundation-copy">{translate('loginCopy', 'Tiếp tục lộ trình bốn kỹ năng và xem tiến bộ của bạn.')}</p>
        <form className="auth-form" aria-describedby={!isLampOn ? 'auth-lamp-lock-message' : undefined} onSubmit={(event) => handleSubmit(event, isLampOn)}>
          <fieldset disabled={!isLampOn}>
            <label htmlFor="login-email">Email</label>
            <input id="login-email" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
          <PasswordFlashlightField
            id="login-password"
            label={translate('password', 'Mật khẩu')}
            showLabel={translate('showPassword', 'Hiện')}
            hideLabel={translate('hidePassword', 'Ẩn')}
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            disabled={!isLampOn}
          />
          {error ? <p className="auth-error" role="alert">{error}</p> : null}
          <Button type="submit" variant="primary" size="lg" disabled={!isLampOn || isSubmitting}>
            {isSubmitting ? 'Đang xử lý…' : translate('login', 'Đăng nhập')}
          </Button>
          </fieldset>
        </form>
        <p className="auth-footer-copy">{translate('noAccount', 'Chưa có tài khoản?')} <Link to="/register">{translate('createAccount', 'Tạo tài khoản')}</Link></p>
      </div>}
    </AuthCinematicShell>
  )
}

export default LoginPage
