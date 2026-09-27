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
      className={`ai-tutor-mascot-launcher ${isActivating ? 'ai-tutor-mascot-launcher-activating' : ''}`.trim()}
      type="button"
      aria-label="Mở Trợ giảng AI"
      aria-expanded={open}
      aria-controls="tutor-dialog"
      hidden={open}
      data-pointer-direction={POINTER_DIRECTIONS.includes(pointerDirection) ? pointerDirection : 'center'}
      onClick={handleClick}
      onPointerMove={handlePointerMove}
      onPointerLeave={handlePointerLeave}
    >
      <span className="ai-tutor-mascot" aria-hidden="true">
        <span className="ai-tutor-mascot-aura" />
        <span className="ai-tutor-mascot-head">
          <span className="ai-tutor-mascot-ear ai-tutor-mascot-ear-left" />
          <span className="ai-tutor-mascot-ear ai-tutor-mascot-ear-right" />
          <span className="ai-tutor-mascot-hair" />
          <span className="ai-tutor-mascot-face">
            <span className="ai-tutor-mascot-glasses ai-tutor-mascot-glasses-left" />
            <span className="ai-tutor-mascot-glasses ai-tutor-mascot-glasses-right" />
            <span className="ai-tutor-mascot-bridge" />
            <span className="ai-tutor-mascot-eye ai-tutor-mascot-eye-left" />
            <span className="ai-tutor-mascot-eye ai-tutor-mascot-eye-right" />
            <span className="ai-tutor-mascot-smile" />
          </span>
        </span>
        <span className="ai-tutor-mascot-collar" />
        <span className="ai-tutor-mascot-book" />
      </span>
      <span className="ai-tutor-mascot-tooltip" role="tooltip">Trợ giảng AI</span>
    </button>
  )
}

export default AiTutorMascotLauncher
