import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import Button from '../components/common/Button'
import AuthCinematicShell from '../components/auth/AuthCinematicShell'
import PasswordFlashlightField from '../components/auth/PasswordFlashlightField'
import { useAuth } from '../features/auth/AuthProvider'
import { useOptionalPreferences } from '../features/preferences/PreferenceProvider'

function RegisterPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { register } = useAuth()
  const preferenceContext = useOptionalPreferences()
  const translate = preferenceContext?.translate ?? ((_key, fallback) => fallback)
  const [firstName, setFirstName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirmation, setPasswordConfirmation] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event, isLampOn) {
    event.preventDefault()
    if (!isLampOn) return
    setError('')
    if (password !== passwordConfirmation) {
      setError('Mật khẩu xác nhận không khớp.')
      return
    }
    setIsSubmitting(true)
    try {
      await register({ firstName, email, password })
      navigate(location.state?.from ?? '/', { replace: true })
    } catch (submissionError) {
      setError(submissionError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthCinematicShell labelledBy="register-title">
      {({ isLampOn }) => <div className="auth-card">
        <p className="eyebrow">LUMEN IELTS · AI TUTOR</p>
        <h1 id="register-title" className="font-display">{translate('register', 'Tạo tài khoản')}</h1>
        <p className="foundation-copy">{translate('registerCopy', 'Lưu bài luyện và theo dõi tiến bộ bốn kỹ năng trong một lộ trình riêng.')}</p>
        <form className="auth-form" aria-describedby={!isLampOn ? 'auth-lamp-lock-message' : undefined} onSubmit={(event) => handleSubmit(event, isLampOn)}>
          <fieldset disabled={!isLampOn}>
            <label htmlFor="register-name">{translate('displayName', 'Tên hiển thị')}</label>
            <input id="register-name" type="text" autoComplete="given-name" value={firstName} onChange={(event) => setFirstName(event.target.value)} required />
            <label htmlFor="register-email">Email</label>
            <input id="register-email" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
          <PasswordFlashlightField
            id="register-password"
            label={translate('password', 'Mật khẩu')}
            showLabel={translate('showPassword', 'Hiện')}
            hideLabel={translate('hidePassword', 'Ẩn')}
            autoComplete="new-password"
            minLength="8"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            disabled={!isLampOn}
          />
          <PasswordFlashlightField
            id="register-password-confirmation"
            label={translate('confirmPassword', 'Xác nhận mật khẩu')}
            visibilityLabel={translate('passwordConfirmationVisibility', 'mật khẩu xác nhận')}
            showLabel={translate('showPassword', 'Hiện')}
            hideLabel={translate('hidePassword', 'Ẩn')}
            autoComplete="new-password"
            minLength="8"
            value={passwordConfirmation}
            onChange={(event) => setPasswordConfirmation(event.target.value)}
            disabled={!isLampOn}
          />
          {error ? <p className="auth-error" role="alert">{error}</p> : null}
          <Button type="submit" variant="primary" size="lg" disabled={!isLampOn || isSubmitting}>{isSubmitting ? 'Đang xử lý…' : translate('register', 'Tạo tài khoản')}</Button>
          </fieldset>
        </form>
        <p className="auth-footer-copy">{translate('hasAccount', 'Đã có tài khoản?')} <Link to="/login">{translate('login', 'Đăng nhập')}</Link></p>
      </div>}
    </AuthCinematicShell>
  )
}

export default RegisterPage
