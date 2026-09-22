import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import Button from '../components/common/Button'
import { useAuth } from '../features/auth/AuthProvider'

function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
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
    <section className="auth-page" aria-labelledby="login-title">
      <div className="auth-card glass-card">
        <p className="eyebrow">IELTS AI TUTOR</p>
        <h1 id="login-title" className="font-display">Đăng nhập</h1>
        <p className="foundation-copy">Tiếp tục lộ trình bốn kỹ năng và xem tiến bộ của bạn.</p>
        <form className="auth-form" onSubmit={handleSubmit}>
          <label htmlFor="login-email">Email</label>
          <input id="login-email" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
          <label htmlFor="login-password">Mật khẩu</label>
          <input id="login-password" type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required />
          {error ? <p className="auth-error" role="alert">{error}</p> : null}
          <Button type="submit" variant="primary" size="lg" disabled={isSubmitting}>
            {isSubmitting ? 'Đang xử lý…' : 'Đăng nhập'}
          </Button>
        </form>
        <p className="auth-footer-copy">Chưa có tài khoản? <Link to="/register">Tạo tài khoản</Link></p>
      </div>
    </section>
  )
}

export default LoginPage
