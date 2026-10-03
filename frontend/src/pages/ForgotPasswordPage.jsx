import { Link } from 'react-router-dom'
import AuthCinematicShell from '../components/auth/AuthCinematicShell'

export default function ForgotPasswordPage() {
  return <AuthCinematicShell labelledBy="forgot-password-title">{() => <div className="auth-card">
    <p className="eyebrow">LUMEN IELTS · ACCOUNT</p>
    <h1 id="forgot-password-title" className="font-display">Đặt lại mật khẩu</h1>
    <p className="foundation-copy">Hạ tầng gửi email đặt lại mật khẩu chưa được cấu hình trong môi trường hiện tại. Không có email giả được gửi.</p>
    <div className="auth-honest-state" role="status">Bạn có thể liên hệ quản trị viên để được hỗ trợ khi hệ thống mail được bật.</div>
    <p className="auth-footer-copy"><Link to="/login">Quay lại đăng nhập</Link></p>
  </div>}</AuthCinematicShell>
}
