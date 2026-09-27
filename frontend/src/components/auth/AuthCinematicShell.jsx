import { useRef, useState } from 'react'

function AuthCinematicShell({ children, labelledBy }) {
  const [isLampOn, setIsLampOn] = useState(false)
  const [isPulling, setIsPulling] = useState(false)
  const [pullDistance, setPullDistance] = useState(0)
  const pullStartRef = useRef(null)
  const pullDistanceRef = useRef(0)
  const pullDragRef = useRef(false)
  const suppressClickRef = useRef(false)

  const toggleLamp = () => setIsLampOn((on) => !on)

  const handlePointerDown = (event) => {
    pullStartRef.current = event.clientY
    pullDragRef.current = false
    setIsPulling(true)
    event.currentTarget.setPointerCapture?.(event.pointerId)
  }

  const handlePointerMove = (event) => {
    if (pullStartRef.current === null) return
    const distance = Math.max(0, Math.min(24, event.clientY - pullStartRef.current))
    pullDistanceRef.current = distance
    setPullDistance(distance)
    pullDragRef.current = distance >= 8
  }

  const handlePointerEnd = (event) => {
    if (pullStartRef.current === null) return
    const didPull = pullDragRef.current && pullDistanceRef.current >= 16
    event.currentTarget.releasePointerCapture?.(event.pointerId)
    pullStartRef.current = null
    pullDistanceRef.current = 0
    pullDragRef.current = false
    setIsPulling(false)
    setPullDistance(0)
    if (didPull) {
      suppressClickRef.current = true
      toggleLamp()
    }
  }

  const handleClick = () => {
    if (suppressClickRef.current) {
      suppressClickRef.current = false
      return
    }
    toggleLamp()
  }

  return (
    <section className={`auth-page auth-cinematic auth-full-bleed ${isLampOn ? 'auth-lamp-on' : 'auth-lamp-off'}`.trim()} aria-labelledby={labelledBy}>
      <div className="auth-lamp-column">
        <div className="auth-lamp-scene">
          <span className="auth-light-cone" data-testid="auth-light-cone" aria-hidden="true" />
          <span className="auth-light-spill" aria-hidden="true" />
          <span className="auth-lamp-halo" aria-hidden="true" />
          <svg className="auth-desk-lamp" data-testid="auth-desk-lamp" viewBox="0 0 220 260" focusable="false" aria-hidden="true">
            <defs>
              <linearGradient id="authLampShadeMetal" x1="0" x2="1" y1="0" y2="1">
                <stop offset="0" stopColor="#101a2d" />
                <stop offset="0.54" stopColor="#263149" />
                <stop offset="1" stopColor="#0a1222" />
              </linearGradient>
              <linearGradient id="authLampBaseMetal" x1="0" x2="0" y1="0" y2="1">
                <stop offset="0" stopColor="#2e3950" />
                <stop offset="1" stopColor="#0a1222" />
              </linearGradient>
              <radialGradient id="authLampShadeGlow" cx="50%" cy="46%" r="60%">
                <stop offset="0" stopColor="#fff0b4" stopOpacity="0.96" />
                <stop offset="0.5" stopColor="#e5c982" stopOpacity="0.4" />
                <stop offset="1" stopColor="#e5c982" stopOpacity="0" />
              </radialGradient>
              <filter id="authLampSvgShadow" x="-30%" y="-30%" width="160%" height="180%">
                <feGaussianBlur stdDeviation="4" />
              </filter>
            </defs>
            <ellipse className="auth-lamp-shadow" cx="110" cy="238" rx="76" ry="9" filter="url(#authLampSvgShadow)" />
            <path className="auth-lamp-shade" d="M18 62C32 34 70 18 110 18s78 16 92 44l-15 58c-25 14-51 21-77 21s-52-7-77-21L18 62Z" fill="url(#authLampShadeMetal)" />
            <path className="auth-lamp-shade-highlight" d="M32 61C50 40 77 29 110 29s60 11 78 32" />
            <path className="auth-lamp-shade-rim" d="M33 116Q110 151 187 116" />
            <path className="auth-lamp-shade-inner" d="M34 113Q110 145 186 113Q170 133 110 140Q50 133 34 113Z" fill="url(#authLampShadeGlow)" />
            <path className="auth-lamp-stem" d="M110 138V216" />
            <path className="auth-lamp-stem-highlight" d="M116 143V214" />
            <path className="auth-lamp-pull" d="M147 116V157" />
            <circle className="auth-lamp-pull-anchor" cx="147" cy="157" r="2.5" />
            <path className="auth-lamp-base" d="M43 220Q110 207 177 220l9 14q-76 19-152 0l9-14Z" fill="url(#authLampBaseMetal)" />
            <path className="auth-lamp-base-highlight" d="M49 222Q110 213 171 222" />
            <ellipse className="auth-lamp-bulb-ring" cx="110" cy="116" rx="21" ry="9" />
            <circle className="auth-lamp-bulb" cx="110" cy="116" r="14" />
            <circle className="auth-lamp-bulb-core" cx="110" cy="116" r="5" />
          </svg>
          <button
            type="button"
            className={`auth-lamp-pull-control ${isPulling ? 'is-pulling' : ''}`.trim()}
            style={{ '--pull-distance': `${pullDistance}px` }}
            aria-label={isLampOn ? 'Tắt đèn bàn học' : 'Bật đèn bàn học'}
            aria-pressed={isLampOn}
            onClick={handleClick}
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={handlePointerEnd}
            onPointerCancel={handlePointerEnd}
          >
            <span className="auth-lamp-cord" aria-hidden="true" />
            <span className="auth-lamp-pull-bead" aria-hidden="true" />
          </button>
        </div>
      </div>
      <div
        className="auth-cinematic-card glass-card"
        data-testid="auth-form-card"
        data-visibility={isLampOn ? 'visible' : 'hidden'}
      >
        {children}
      </div>
    </section>
  )
}

export default AuthCinematicShell
