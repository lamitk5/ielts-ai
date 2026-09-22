import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import Button from '../components/common/Button'
import { useAuth } from '../features/auth/AuthProvider'

function RegisterPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { register } = useAuth()
  const [firstName, setFirstName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
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
    <section className="auth-page" aria-labelledby="register-title">
      <div className="auth-card glass-card">
        <p className="eyebrow">IELTS AI TUTOR</p>
        <h1 id="register-title" className="font-display">Tạo tài khoản</h1>
        <p className="foundation-copy">Lưu bài luyện và theo dõi tiến bộ bốn kỹ năng trong một lộ trình riêng.</p>
        <form className="auth-form" onSubmit={handleSubmit}>
          <label htmlFor="register-name">Tên hiển thị</label>
          <input id="register-name" type="text" autoComplete="given-name" value={firstName} onChange={(event) => setFirstName(event.target.value)} required />
          <label htmlFor="register-email">Email</label>
          <input id="register-email" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
          <label htmlFor="register-password">Mật khẩu</label>
          <input id="register-password" type="password" autoComplete="new-password" minLength="8" value={password} onChange={(event) => setPassword(event.target.value)} required />
          {error ? <p className="auth-error" role="alert">{error}</p> : null}
          <Button type="submit" variant="primary" size="lg" disabled={isSubmitting}>{isSubmitting ? 'Đang xử lý…' : 'Tạo tài khoản'}</Button>
        </form>
        <p className="auth-footer-copy">Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p>
      </div>
    </section>
  )
}

export default RegisterPage
