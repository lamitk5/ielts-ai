import { useEffect, useRef, useState } from 'react'
import { useEffectiveReducedMotion } from '../../features/preferences/PreferenceProvider'

function getPointerDirection(rect, clientX, clientY) {
  const horizontal = clientX - (rect.left + rect.width / 2)
  const vertical = clientY - (rect.top + rect.height / 2)

  if (Math.abs(horizontal) < rect.width * 0.2 && Math.abs(vertical) < rect.height * 0.2) return 'center'
  if (Math.abs(horizontal) > Math.abs(vertical)) return horizontal > 0 ? 'right' : 'left'
  return vertical > 0 ? 'below' : 'above'
}

function LumenScholar({ prefersReducedMotion }) {
  return (
    <svg className="ai-tutor-mascot" data-testid="lumen-scholar-mascot" data-idle-motion={prefersReducedMotion ? 'disabled' : 'enabled'} viewBox="0 0 120 120" role="img" aria-label="LUMEN Scholar" focusable="false">
      <defs>
        <radialGradient id="lumenScholarAura">
          <stop offset="0" stopColor="var(--accent-soft)" />
          <stop offset="1" stopColor="transparent" />
        </radialGradient>
      </defs>
      <circle className="ai-tutor-mascot-aura" cx="60" cy="60" r="52" fill="url(#lumenScholarAura)" />
      <path className="ai-tutor-mascot-shoulders" d="M22 111c4-19 18-29 38-29s34 10 38 29" fill="var(--navy)" />
      <g className="ai-tutor-mascot-face">
        <path className="ai-tutor-mascot-head" d="M28 48c0-22 14-35 32-35s32 13 32 35v16c0 18-14 29-32 29S28 82 28 64V48Z" fill="var(--navy)" stroke="var(--border-strong)" strokeWidth="1.5" />
        <path className="ai-tutor-mascot-face-plate" d="M35 51c0-13 11-22 25-22s25 9 25 22v12c0 13-11 22-25 22S35 76 35 63V51Z" fill="#f5ead1" />
        <path className="ai-tutor-mascot-cap" d="M24 46c5-25 17-37 36-37s31 12 36 37c-21-7-51-7-72 0Z" fill="var(--navy)" stroke="var(--accent)" strokeWidth="1.5" />
        <path className="ai-tutor-mascot-cap-band" d="M31 40c18-5 40-5 58 0" fill="none" stroke="var(--gold-light)" strokeWidth="1" opacity=".8" />
        <g className="ai-tutor-mascot-eye ai-tutor-mascot-eye-left">
          <circle cx="47" cy="60" r="3.4" fill="#fffaf0" />
          <circle className="ai-tutor-mascot-pupil" data-testid="mascot-pupil-left" cx="47" cy="60" r="1.4" fill="var(--navy)" />
        </g>
        <g className="ai-tutor-mascot-eye ai-tutor-mascot-eye-right">
          <circle cx="73" cy="60" r="3.4" fill="#fffaf0" />
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
    pendingPointerRef.current = { clientX: event.clientX, clientY: event.clientY }
    if (frameRef.current) return
    frameRef.current = requestAnimationFrame(() => {
      frameRef.current = null
      const current = pendingPointerRef.current
      if (!current || !buttonRef?.current) return
      const rect = buttonRef.current.getBoundingClientRect()
      const horizontal = current.clientX - (rect.left + rect.width / 2)
      const vertical = current.clientY - (rect.top + rect.height / 2)
      const x = Math.max(-1, Math.min(1, horizontal / Math.max(1, rect.width / 2))) * 1.2
      const y = Math.max(-1, Math.min(1, vertical / Math.max(1, rect.height / 2))) * 1.2
      const direction = getPointerDirection(rect, current.clientX, current.clientY)
      buttonRef.current.style.setProperty('--mascot-pupil-x', `${Number(x.toFixed(2))}px`)
      buttonRef.current.style.setProperty('--mascot-pupil-y', `${Number(y.toFixed(2))}px`)
      buttonRef.current.dataset.pointerDirection = direction
    })
  }

  function handlePointerLeave() {
    if (frameRef.current) cancelAnimationFrame(frameRef.current)
    frameRef.current = null
    pendingPointerRef.current = null
    buttonRef.current?.style.setProperty('--mascot-pupil-x', '0px')
    buttonRef.current?.style.setProperty('--mascot-pupil-y', '0px')
    if (buttonRef.current) buttonRef.current.dataset.pointerDirection = 'center'
  }

  function handleClick() {
    if (prefersReducedMotion) {
      onClick?.()
      return
    }
    if (activationTimerRef.current) window.clearTimeout(activationTimerRef.current)
    setIsActivating(true)
    activationTimerRef.current = window.setTimeout(() => {
      activationTimerRef.current = null
      setIsActivating(false)
      onClick?.()
    }, 260)
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
      data-pointer-direction="center"
      onClick={handleClick}
      onPointerMove={handlePointerMove}
      onPointerLeave={handlePointerLeave}
    >
      <LumenScholar prefersReducedMotion={prefersReducedMotion} />
      <span className="ai-tutor-mascot-tooltip" role="tooltip">Trợ giảng AI</span>
    </button>
  )
}

export default AiTutorMascotLauncher
