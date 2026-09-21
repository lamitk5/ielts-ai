import { Link } from 'react-router-dom'

const footerLinks = [
  { label: 'Home', to: '/' },
  { label: 'Reading', to: '/reading' },
  { label: 'Listening', to: '/listening' },
  { label: 'Writing', to: '/writing' },
  { label: 'Speaking', to: '/speaking' },
]

function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-content">
        <div>
          <Link className="footer-brand" to="/">
            IELTS AI
          </Link>
          <p className="footer-description">
            A calm academic AI foundation for four-skill IELTS practice.
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
      <p className="footer-meta">Built for Reading, Listening, Writing, and Speaking.</p>
    </footer>
  )
}

export default Footer
