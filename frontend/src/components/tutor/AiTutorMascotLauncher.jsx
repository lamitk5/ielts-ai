import { useEffect, useRef, useState } from 'react'
import { useEffectiveReducedMotion } from '../../features/preferences/PreferenceProvider'

const POINTER_DIRECTIONS = ['left', 'right', 'above', 'below', 'center']

function getPointerDirection(rect, clientX, clientY) {
  const horizontal = clientX - (rect.left + rect.width / 2)
  const vertical = clientY - (rect.top + rect.height / 2)

  if (Math.abs(horizontal) < rect.width * 0.2 && Math.abs(vertical) < rect.height * 0.2) return 'center'
  if (Math.abs(horizontal) > Math.abs(vertical)) return horizontal > 0 ? 'right' : 'left'
  return vertical > 0 ? 'below' : 'above'
}

function LumenScholar({ prefersReducedMotion }) {
  return (
    <svg className="ai-tutor-mascot" data-testid="lumen-scholar-mascot" viewBox="0 0 120 120" role="img" aria-label="LUMEN Scholar" focusable="false">
      <defs>
        <linearGradient id="lumenScholarCowl" x1="0" x2="1" y1="0" y2="1">
          <stop offset="0" stopColor="var(--surface-elevated)" />
          <stop offset="1" stopColor="var(--navy-light)" />
        </linearGradient>
        <radialGradient id="lumenScholarAura">
          <stop offset="0" stopColor="var(--accent-soft)" />
          <stop offset="1" stopColor="transparent" />
        </radialGradient>
      </defs>
      <circle className="ai-tutor-mascot-aura" cx="60" cy="60" r="52" fill="url(#lumenScholarAura)" />
      <path className="ai-tutor-mascot-shoulders" d="M22 111c4-19 18-29 38-29s34 10 38 29" fill="var(--navy)" />
      <path className="ai-tutor-mascot-collar" d="m45 86 15 18 15-18" fill="none" stroke="var(--accent)" strokeWidth="2" />
      <path className="ai-tutor-mascot-book" d="M22 108c13-4 25-3 38 3V96c-13-6-25-6-38-1v13Zm76 0c-13-4-25-3-38 3V96c13-6 25-6 38-1v13Z" fill="var(--gold)" opacity=".92" />
      <g className="ai-tutor-mascot-face">
        <path className="ai-tutor-mascot-head" d="M28 48c0-22 14-35 32-35s32 13 32 35v16c0 18-14 29-32 29S28 82 28 64V48Z" fill="url(#lumenScholarCowl)" stroke="var(--border-strong)" strokeWidth="1.5" />
        <path className="ai-tutor-mascot-cap" d="M24 46c5-25 17-37 36-37s31 12 36 37c-21-7-51-7-72 0Z" fill="var(--navy)" stroke="var(--accent)" strokeWidth="1.5" />
        <path className="ai-tutor-mascot-cap-band" d="M31 40c18-5 40-5 58 0" fill="none" stroke="var(--gold-light)" strokeWidth="1" opacity=".8" />
        <path className="ai-tutor-mascot-glasses ai-tutor-mascot-glasses-left" d="M35 54h19c2 0 3 2 2 4l-1 5c0 3-2 4-5 4H40c-3 0-5-2-5-5l-1-5c0-2 0-3 1-3Z" fill="rgba(7,20,38,.35)" stroke="var(--gold-light)" />
        <path className="ai-tutor-mascot-glasses ai-tutor-mascot-glasses-right" d="M66 54h19c1 0 1 1 1 3l-1 5c0 3-2 5-5 5h-10c-3 0-5-1-5-4l-1-5c-1-2 0-4 2-4Z" fill="rgba(7,20,38,.35)" stroke="var(--gold-light)" />
        <path className="ai-tutor-mascot-bridge" d="M56 58h9" stroke="var(--gold-light)" />
        <g className="ai-tutor-mascot-eye ai-tutor-mascot-eye-left">
          <circle cx="47" cy="60" r="2.8" fill="var(--text)" />
          <circle className="ai-tutor-mascot-pupil" data-testid="mascot-pupil-left" cx="47" cy="60" r="1.4" fill="var(--navy)" />
        </g>
        <g className="ai-tutor-mascot-eye ai-tutor-mascot-eye-right">
          <circle cx="73" cy="60" r="2.8" fill="var(--text)" />
          <circle className="ai-tutor-mascot-pupil" data-testid="mascot-pupil-right" cx="73" cy="60" r="1.4" fill="var(--navy)" />
        </g>
        <path className="ai-tutor-mascot-smile" d="M53 78c5 4 9 4 14 0" fill="none" stroke="var(--gold-light)" strokeWidth="1.5" strokeLinecap="round" />
      </g>
      {!prefersReducedMotion ? <circle className="ai-tutor-mascot-star" cx="98" cy="22" r="2" fill="var(--gold-light)" /> : null}
    </svg>
  )
}

function AiTutorMascotLauncher({ onClick, buttonRef, open = false }) {
  const prefersReducedMotion = useEffectiveReducedMotion()
  const [pointerDirection, setPointerDirection] = useState('center')
  const [isActivating, setIsActivating] = useState(false)
  const frameRef = useRef(null)
  const pendingPointerRef = useRef(null)
  const activationTimerRef = useRef(null)

  useEffect(() => () => {
    if (frameRef.current) cancelAnimationFrame(frameRef.current)
    if (activationTimerRef.current) window.clearTimeout(activationTimerRef.current)
  }, [])

  function handlePointerMove(event) {
    if (prefersReducedMotion || event.pointerType === 'touch') return
    pendingPointerRef.current = event
    if (frameRef.current) return
    frameRef.current = requestAnimationFrame(() => {
      frameRef.current = null
      const current = pendingPointerRef.current
      if (!current || !buttonRef?.current) return
      const rect = buttonRef.current.getBoundingClientRect()
      setPointerDirection(getPointerDirection(rect, current.clientX, current.clientY))
    })
  }

  function handlePointerLeave() {
    if (frameRef.current) cancelAnimationFrame(frameRef.current)
    frameRef.current = null
    pendingPointerRef.current = null
    setPointerDirection('center')
  }

  function handleClick() {
    if (!prefersReducedMotion) {
      setIsActivating(true)
      activationTimerRef.current = window.setTimeout(() => setIsActivating(false), 260)
    }
    onClick?.()
  }

  return (
    <button
      ref={buttonRef}
      className={`ai-tutor-mascot-launcher ${isActivating ? 'ai-tutor-mascot-launcher-activating' : ''} ${open && isActivating ? 'ai-tutor-mascot-launcher-open-reaction' : ''}`.trim()}
      type="button"
      aria-label="Mở Trợ giảng AI"
      aria-expanded={open}
      aria-controls="tutor-dialog"
      aria-hidden={open ? 'true' : undefined}
      tabIndex={open ? -1 : undefined}
      hidden={open && !isActivating}
      data-mascot="lumen-scholar"
      data-pointer-direction={POINTER_DIRECTIONS.includes(pointerDirection) ? pointerDirection : 'center'}
      onClick={handleClick}
      onPointerMove={handlePointerMove}
      onPointerLeave={handlePointerLeave}
    >
      <LumenScholar prefersReducedMotion={prefersReducedMotion} />
      <span className="ai-tutor-mascot-tooltip" role="tooltip">LUMEN Scholar</span>
    </button>
  )
}

export default AiTutorMascotLauncher
