import { Menu, X } from 'lucide-react'
import { useState } from 'react'
import { NavLink } from 'react-router-dom'

const links = [
  { label: 'Home', to: '/' },
  { label: 'Reading', to: '/reading' },
  { label: 'Listening', to: '/listening' },
  { label: 'Writing', to: '/writing' },
  { label: 'Speaking', to: '/speaking' },
  { label: 'AI Tutor', to: '/tutor' },
]

function Navbar() {
  const [isMenuOpen, setIsMenuOpen] = useState(false)

  const closeMenu = () => setIsMenuOpen(false)

  return (
    <header className="site-header">
      <nav className="site-nav" aria-label="Primary navigation">
        <NavLink className="brand" to="/" onClick={closeMenu}>
          IELTS AI
        </NavLink>

        <button
          type="button"
          className="menu-toggle"
          aria-expanded={isMenuOpen}
          aria-controls="primary-navigation"
          aria-label={isMenuOpen ? 'Close navigation menu' : 'Open navigation menu'}
          onClick={() => setIsMenuOpen((open) => !open)}
        >
          {isMenuOpen ? <X aria-hidden="true" /> : <Menu aria-hidden="true" />}
        </button>

        <div
          id="primary-navigation"
          className={`nav-content ${isMenuOpen ? 'nav-content-open' : ''}`.trim()}
        >
          <div className="nav-links">
            {links.map((link) => (
              <NavLink
                key={link.label}
                className={({ isActive }) =>
                  `nav-link ${isActive ? 'nav-link-active' : ''}`.trim()
                }
                to={link.to}
                onClick={closeMenu}
              >
                {link.label}
              </NavLink>
            ))}
          </div>
          <NavLink className="signin-link" to="/signin" onClick={closeMenu}>
            Sign in
          </NavLink>
        </div>
      </nav>
    </header>
  )
}

export default Navbar
