import { Link } from 'react-router-dom'

const footerLinks = [
  { label: 'Trang chủ', to: '/' },
  { label: '4 kỹ năng', to: '/#skills' },
  { label: 'Trợ giảng AI', to: '/#ai-tutor' },
  { label: 'Tiến độ', to: '/#progress' },
]

function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-content">
        <div>
          <Link className="footer-brand" to="/">
            IELTS AI Tutor
          </Link>
          <p className="footer-description">
            Trợ giảng AI hỗ trợ luyện tập Reading, Listening, Writing và Speaking.
          </p>
        </div>
        <nav aria-label="Footer navigation" className="footer-links">
          {footerLinks.map((link) => (
            <Link key={link.label} to={link.to}>
              {link.label}
            </Link>
          ))}
        </nav>
      </div>
      <p className="footer-meta">A focused foundation for four-skill IELTS learning.</p>
    </footer>
  )
}

export default Footer
