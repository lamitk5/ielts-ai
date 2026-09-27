import { useEffect, useRef, useState } from 'react'
import { useEffectiveReducedMotion } from '../../features/preferences/PreferenceProvider'
import LumenPixelScholarMascot from './LumenPixelScholarMascot'

function getPointerDirection(rect, clientX, clientY) {
  const horizontal = clientX - (rect.left + rect.width / 2)
  const vertical = clientY - (rect.top + rect.height / 2)

  if (Math.abs(horizontal) < rect.width * 0.2 && Math.abs(vertical) < rect.height * 0.2) return 'center'
  if (Math.abs(horizontal) > Math.abs(vertical)) return horizontal > 0 ? 'right' : 'left'
  return vertical > 0 ? 'below' : 'above'
}

const PUPIL_TRAVEL = 4
const HEAD_TRAVEL = 1.5
const HEAD_ROTATION = 2.5
const TRACKING_DISTANCE = 180

function getTrackingVector(rect, clientX, clientY) {
  const dx = clientX - (rect.left + rect.width / 2)
  const dy = clientY - (rect.top + rect.height / 2)
  const distance = Math.hypot(dx, dy)
  if (!distance) return { x: 0, y: 0, intensity: 0 }

  const intensity = Math.min(1, distance / TRACKING_DISTANCE)
  return { x: (dx / distance) * intensity, y: (dy / distance) * intensity, intensity }
}

function AiTutorMascotLauncher({ onClick, buttonRef, open = false }) {
  const prefersReducedMotion = useEffectiveReducedMotion()
  const [isActivating, setIsActivating] = useState(false)
  const frameRef = useRef(null)
  const pendingPointerRef = useRef(null)
  const activationTimerRef = useRef(null)

  useEffect(() => {
    function handleGlobalPointerMove(event) {
      if (event.pointerType && event.pointerType !== 'mouse' && event.pointerType !== 'pen') return
      pendingPointerRef.current = { clientX: event.clientX, clientY: event.clientY }
      if (frameRef.current) return
      frameRef.current = requestAnimationFrame(() => {
        frameRef.current = null
        const current = pendingPointerRef.current
        if (!current || !buttonRef?.current) return
        const rect = buttonRef.current.getBoundingClientRect()
        const vector = getTrackingVector(rect, current.clientX, current.clientY)
        const direction = getPointerDirection(rect, current.clientX, current.clientY)
        const mascot = buttonRef.current.querySelector('.ai-tutor-mascot')
        if (!mascot) return
        const style = mascot.style
        style.setProperty('--mascot-pupil-x', `${Number((vector.x * PUPIL_TRAVEL).toFixed(2))}px`)
        style.setProperty('--mascot-pupil-y', `${Number((vector.y * PUPIL_TRAVEL).toFixed(2))}px`)
        style.setProperty('--mascot-head-x', `${Number((vector.x * HEAD_TRAVEL).toFixed(2))}px`)
        style.setProperty('--mascot-head-y', `${Number((vector.y * HEAD_TRAVEL).toFixed(2))}px`)
        style.setProperty('--mascot-head-rotate', `${Number((vector.x * vector.intensity * HEAD_ROTATION).toFixed(2))}deg`)
        buttonRef.current.dataset.pointerDirection = direction
      })
    }

    window.addEventListener('pointermove', handleGlobalPointerMove, { passive: true })
    return () => {
      window.removeEventListener('pointermove', handleGlobalPointerMove)
      if (frameRef.current) cancelAnimationFrame(frameRef.current)
      frameRef.current = null
      pendingPointerRef.current = null
    }
  }, [buttonRef, prefersReducedMotion])

  useEffect(() => () => {
    if (activationTimerRef.current) window.clearTimeout(activationTimerRef.current)
  }, [])

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
      data-mascot="lumen-pixel-scholar"
      data-pointer-direction="center"
      onClick={handleClick}
    >
      <LumenPixelScholarMascot prefersReducedMotion={prefersReducedMotion} isBlinking={isActivating} />
      <span className="ai-tutor-mascot-tooltip" role="tooltip">Trợ giảng AI</span>
    </button>
  )
}

export default AiTutorMascotLauncher
