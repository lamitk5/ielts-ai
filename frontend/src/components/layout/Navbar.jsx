import { ChevronDown, Menu, UserCircle, X } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '../../features/auth/AuthProvider'
import { PreferenceProvider, applyPreferenceTokens } from '../../features/preferences/PreferenceProvider'
import { DEFAULT_PREFERENCES } from '../../features/preferences/preferenceDefaults'
import SettingsButton from '../settings/SettingsButton'
import SettingsDrawer from '../settings/SettingsDrawer'
import LumenLogo from './LumenLogo'

const links = [
  { label: 'Trang chủ', to: '/' },
  { label: '4 kỹ năng', to: '/#skills' },
  { label: 'Trợ giảng AI', to: '/#ai-tutor' },
  { label: 'Tiến độ', to: '/#progress' },
]

function isAnchorActive(link, location) {
  const [path, hash] = link.to.split('#')
  if (hash) return location.pathname === path && location.hash === `#${hash}`
  return location.pathname === path && !location.hash
}

function Navbar() {
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const [isAccountOpen, setIsAccountOpen] = useState(false)
  const [settingsRoute, setSettingsRoute] = useState(null)
  const [isScrolled, setIsScrolled] = useState(() => window.scrollY > 12)
  const settingsOpenerRef = useRef(null)
  const { isAuthenticated, logout } = useAuth()
  const location = useLocation()
  const isSettingsOpen = settingsRoute === location.key

  useEffect(() => {
    const handleScroll = () => setIsScrolled(window.scrollY > 12)
    window.addEventListener('scroll', handleScroll, { passive: true })
    handleScroll()
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  const closeMenu = () => setIsMenuOpen(false)

  useEffect(() => {
    if (!isMenuOpen) return undefined
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') closeMenu()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isMenuOpen])

  const handleLogout = async () => {
    closeMenu()
    setIsAccountOpen(false)
    try { await logout() } finally { applyPreferenceTokens(DEFAULT_PREFERENCES) }
  }

  return (
    <header className={`site-header site-header-glass ${isScrolled ? 'site-header-scrolled' : ''}`.trim()}>
      <nav className="site-nav" aria-label="Primary navigation" data-menu-open={isMenuOpen ? 'true' : 'false'}>
        <NavLink className="brand" to="/" aria-label="LUMEN IELTS AI Tutor" onClick={closeMenu}>
          <LumenLogo />
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
              <Link
                key={link.label}
                className={`nav-link ${isAnchorActive(link, location) ? 'nav-link-active' : ''}`.trim()}
                aria-current={isAnchorActive(link, location) ? 'page' : undefined}
                to={link.to}
                onClick={closeMenu}
              >
                {link.label}
              </Link>
            ))}
          </div>
          <div className="nav-settings">
            <SettingsButton openerRef={settingsOpenerRef} expanded={isSettingsOpen} onClick={() => { setIsMenuOpen(false); setSettingsRoute(location.key) }} />
          </div>
          {isAuthenticated ? (
            <div className="account-menu">
              <button
                type="button"
                className="account-trigger"
                aria-expanded={isAccountOpen}
                aria-haspopup="menu"
                aria-label="Mở menu tài khoản"
                onClick={() => setIsAccountOpen((open) => !open)}
              >
                <UserCircle aria-hidden="true" />
                <span>Tài khoản</span>
                <ChevronDown aria-hidden="true" className={isAccountOpen ? 'account-chevron-open' : ''} />
              </button>
              {isAccountOpen ? (
                <div className="account-panel" role="menu" aria-label="Menu tài khoản">
                  <span className="account-email" role="presentation">Tài khoản học tập</span>
                  <button className="account-menu-item" role="menuitem" type="button" onClick={handleLogout}>
                    Đăng xuất
                  </button>
                </div>
              ) : null}
            </div>
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
