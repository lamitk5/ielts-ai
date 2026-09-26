import { Menu, X } from 'lucide-react'
import { useRef, useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '../../features/auth/AuthProvider'
import { PreferenceProvider, applyPreferenceTokens } from '../../features/preferences/PreferenceProvider'
import { DEFAULT_PREFERENCES } from '../../features/preferences/preferenceDefaults'
import SettingsButton from '../settings/SettingsButton'
import SettingsDrawer from '../settings/SettingsDrawer'

const links = [
  { label: 'Trang chủ', to: '/' },
  { label: '4 kỹ năng', to: '/#skills' },
  { label: 'Trợ giảng AI', to: '/#ai-tutor' },
  { label: 'Tiến độ', to: '/#progress' },
]

function Navbar() {
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const [settingsRoute, setSettingsRoute] = useState(null)
  const settingsOpenerRef = useRef(null)
  const { isAuthenticated, logout } = useAuth()
  const location = useLocation()
  const isSettingsOpen = settingsRoute === location.key

  const closeMenu = () => setIsMenuOpen(false)

  const handleLogout = async () => {
    closeMenu()
    try { await logout() } finally { applyPreferenceTokens(DEFAULT_PREFERENCES) }
  }

  return (
    <header className="site-header">
      <nav className="site-nav" aria-label="Primary navigation">
        <NavLink className="brand" to="/" onClick={closeMenu}>
          IELTS AI Tutor
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

        <div className="nav-settings">
          <SettingsButton openerRef={settingsOpenerRef} expanded={isSettingsOpen} onClick={() => { setIsMenuOpen(false); setSettingsRoute(location.key) }} />
        </div>

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
          {isAuthenticated ? (
            <button className="signin-link nav-auth-button" type="button" onClick={handleLogout}>
              Đăng xuất
            </button>
          ) : (
            <NavLink className="signin-link" to="/login" onClick={closeMenu}>
              Đăng nhập
            </NavLink>
          )}
        </div>
      </nav>
      {(isAuthenticated || isSettingsOpen) && (
        <PreferenceProvider>
          <SettingsDrawer open={isSettingsOpen} onClose={() => setSettingsRoute(null)} openerRef={settingsOpenerRef} />
        </PreferenceProvider>
      )}
    </header>
  )
}

export default Navbar
