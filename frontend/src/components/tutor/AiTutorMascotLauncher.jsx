import { useEffect, useRef, useState } from 'react'
import { useEffectiveReducedMotion } from '../../features/preferences/PreferenceProvider'
import { ASSISTANT_NAME } from '../../features/tutor/assistantIdentity'
import LumenScholarMascot from './LumenScholarMascot'

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
const REMINDER_LINES = [
  'Học đi bạn ê 👀',
  `Biết là bận rồi… nhưng chú ý ${ASSISTANT_NAME} một chút.`,
  'Hmmm…',
  'Làm thêm 1 bài nữa thôi.',
  'Ê, Reading đang đợi kìa.',
  `Đừng bỏ ${ASSISTANT_NAME} ở góc này chứ 🥲`,
  `${ASSISTANT_NAME} nhớ bạn rồi đấy.`,
  `Nhìn ${ASSISTANT_NAME} một chút đi.`,
  `Cần ${ASSISTANT_NAME} gợi ý bài tiếp theo không?`,
  '10 giây rồi đấy… học tí nào.',
  `${ASSISTANT_NAME} vẫn đang nhìn đấy nhé 👀`,
]

function getTrackingVector(rect, clientX, clientY) {
  const dx = clientX - (rect.left + rect.width / 2)
  const dy = clientY - (rect.top + rect.height / 2)
  const distance = Math.hypot(dx, dy)
  if (!distance) return { x: 0, y: 0, intensity: 0 }

  const intensity = Math.min(1, distance / TRACKING_DISTANCE)
  return { x: (dx / distance) * intensity, y: (dy / distance) * intensity, intensity }
}

function AiTutorMascotLauncher({ onClick, buttonRef, open = false, proactiveAiEnabled = true }) {
  const prefersReducedMotion = useEffectiveReducedMotion()
  const [isActivating, setIsActivating] = useState(false)
  const [reminder, setReminder] = useState(null)
  const frameRef = useRef(null)
  const pendingPointerRef = useRef(null)
  const activationTimerRef = useRef(null)
  const reminderTimerRef = useRef(null)
  const reminderHideTimerRef = useRef(null)

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

  useEffect(() => {
    if (reminderTimerRef.current) window.clearTimeout(reminderTimerRef.current)
    if (reminderHideTimerRef.current) window.clearTimeout(reminderHideTimerRef.current)
    if (!proactiveAiEnabled || open) return undefined

    let disposed = false

    function scheduleReminder() {
      const delay = 9000 + Math.round(Math.random() * 3000)
      reminderTimerRef.current = window.setTimeout(() => {
        if (disposed) return
        const line = REMINDER_LINES[Math.floor(Math.random() * REMINDER_LINES.length)]
        setReminder(line)
        reminderHideTimerRef.current = window.setTimeout(() => setReminder(null), 5000)
        scheduleReminder()
      }, delay)
    }

    scheduleReminder()
    return () => {
      disposed = true
      if (reminderTimerRef.current) window.clearTimeout(reminderTimerRef.current)
      if (reminderHideTimerRef.current) window.clearTimeout(reminderHideTimerRef.current)
    }
  }, [open, proactiveAiEnabled])

  function handleClick() {
    setReminder(null)
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

  const visibleReminder = open || !proactiveAiEnabled ? null : reminder

  return (
    <>
      {visibleReminder ? (
        <button
          className="ai-tutor-mascot-reminder"
          type="button"
          aria-label={`Mở ${ASSISTANT_NAME}: ${reminder}`}
          aria-live="polite"
          onClick={handleClick}
        >
          {reminder}
        </button>
      ) : null}
      <button
        ref={buttonRef}
        className={`ai-tutor-mascot-launcher ${isActivating ? 'ai-tutor-mascot-launcher-activating' : ''} ${open && isActivating ? 'ai-tutor-mascot-launcher-open-reaction' : ''}`.trim()}
        type="button"
        aria-label={`Mở ${ASSISTANT_NAME}`}
        aria-expanded={open}
        aria-controls="tutor-dialog"
        aria-hidden={open ? 'true' : undefined}
        tabIndex={open ? -1 : undefined}
        hidden={open && !isActivating}
        data-mascot="lumen-pixel-owl"
        data-pointer-direction="center"
        onClick={handleClick}
      >
        <LumenScholarMascot prefersReducedMotion={prefersReducedMotion} isBlinking={isActivating} />
        <span className="ai-tutor-mascot-tooltip" role="tooltip">{ASSISTANT_NAME}</span>
      </button>
    </>
  )
}

export default AiTutorMascotLauncher
