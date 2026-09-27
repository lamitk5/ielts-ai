import { useEffect, useState } from 'react'

const idleExpressions = ['thinking', 'sleepy', 'excited', 'pout', 'cry']

function useIdleLife(prefersReducedMotion) {
  const [isBlinking, setIsBlinking] = useState(false)
  const [idleTilt, setIdleTilt] = useState('center')
  const [expression, setExpression] = useState('neutral')

  useEffect(() => {
    let blinkTimer
    let closeTimer
    let doubleBlinkTimer
    let doubleCloseTimer
    let disposed = false

    function closeBlink() {
      if (!disposed) setIsBlinking(false)
    }

    function triggerBlink() {
      if (disposed) return
      setIsBlinking(true)
      closeTimer = window.setTimeout(closeBlink, 180)
      if (Math.random() > 0.72) {
        doubleBlinkTimer = window.setTimeout(() => {
          if (disposed) return
          setIsBlinking(true)
          doubleCloseTimer = window.setTimeout(closeBlink, 160)
        }, 280)
      }
      blinkTimer = window.setTimeout(triggerBlink, 5600 + Math.round(Math.random() * 2200))
    }

    blinkTimer = window.setTimeout(triggerBlink, 5600)
    return () => {
      disposed = true
      window.clearTimeout(blinkTimer)
      window.clearTimeout(closeTimer)
      window.clearTimeout(doubleBlinkTimer)
      window.clearTimeout(doubleCloseTimer)
    }
  }, [])

  useEffect(() => {
    if (prefersReducedMotion) return undefined

    let tiltTimer
    let resetTimer
    let disposed = false

    function triggerTilt() {
      if (disposed) return
      setIdleTilt(Math.random() > 0.5 ? 'left' : 'right')
      resetTimer = window.setTimeout(() => {
        if (!disposed) setIdleTilt('center')
      }, 900)
      tiltTimer = window.setTimeout(triggerTilt, 7200 + Math.round(Math.random() * 3200))
    }

    tiltTimer = window.setTimeout(triggerTilt, 7200)
    return () => {
      disposed = true
      window.clearTimeout(tiltTimer)
      window.clearTimeout(resetTimer)
    }
  }, [prefersReducedMotion])

  useEffect(() => {
    if (prefersReducedMotion) return undefined

    let expressionTimer
    let resetTimer
    let disposed = false
    let expressionIndex = 0

    function triggerExpression() {
      if (disposed) return
      setExpression(idleExpressions[expressionIndex % idleExpressions.length])
      expressionIndex += 1
      resetTimer = window.setTimeout(() => {
        if (!disposed) setExpression('neutral')
      }, 4200)
      expressionTimer = window.setTimeout(triggerExpression, 18000)
    }

    expressionTimer = window.setTimeout(triggerExpression, 18000)
    return () => {
      disposed = true
      window.clearTimeout(expressionTimer)
      window.clearTimeout(resetTimer)
    }
  }, [prefersReducedMotion])

  return { isBlinking, idleTilt, expression }
}

function PixelEye({ side, blinking }) {
  const cx = side === 'left' ? 46 : 82
  const eyeClass = `ai-tutor-mascot-eye ai-tutor-mascot-eye-${side} pixel-owl-eye`

  return (
    <g className={eyeClass} data-blink={blinking ? 'closed' : 'open'}>
      <circle className="pixel-owl-eye-ring" cx={cx} cy="67" r="16" />
      <circle className="pixel-owl-eye-white" cx={cx} cy="67" r="12" />
      <circle
        className="ai-tutor-mascot-pupil pixel-owl-pupil"
        data-testid={`mascot-pupil-${side}`}
        cx={cx}
        cy="67"
        r="8"
        style={{ transform: 'translate(var(--mascot-pupil-x), var(--mascot-pupil-y))' }}
      />
      <rect className="pixel-owl-eye-glint" x={cx - 4} y="61" width="4" height="4" />
      <rect className="pixel-owl-eye-lid" x={cx - 14} y="65" width="28" height="5" />
      <rect className="pixel-owl-tear" x={cx + 10} y="78" width="3" height="5" />
    </g>
  )
}

export default function LumenScholarMascot({ prefersReducedMotion, isBlinking = false }) {
  const { isBlinking: isIdleBlinking, idleTilt, expression } = useIdleLife(prefersReducedMotion)
  const blinking = isBlinking || isIdleBlinking

  return (
    <svg
      className="ai-tutor-mascot ai-tutor-pixel-owl-scholar"
      data-testid="lumen-scholar-mascot"
      data-character="pixel-owl-scholar"
      data-size="bounded"
      data-idle-motion={prefersReducedMotion ? 'disabled' : 'enabled'}
      data-idle-blink={isIdleBlinking ? 'closed' : 'open'}
      data-blink={blinking ? 'closed' : 'open'}
      data-idle-tilt={prefersReducedMotion ? 'center' : idleTilt}
      data-expression={prefersReducedMotion ? 'neutral' : expression}
      viewBox="0 0 128 128"
      shapeRendering="crispEdges"
      role="img"
      aria-label="LUMEN Pixel Owl"
      focusable="false"
    >
      <rect className="ai-tutor-mascot-aura" x="9" y="9" width="110" height="110" />
      <g
        className="ai-tutor-mascot-face"
        style={{ transform: 'translate(var(--mascot-head-x), var(--mascot-head-y)) rotate(var(--mascot-head-rotate))' }}
      >
        <g className="pixel-owl-expression">
          <path className="pixel-owl-robe" d="M16 127v-12l8-8h16l8-8h24l8 8h16l8 8h8v12Z" />
          <path className="pixel-owl-wing pixel-owl-wing-left" d="M38 101H25l-9 12 12 8 18-8Z" />
          <path className="pixel-owl-wing pixel-owl-wing-right" d="M90 101h13l9 12-12 8-18-8Z" />
          <path className="pixel-owl-feather" d="M22 111h15m-12 5h14m72-5H96m10 5H92" />
          <path className="pixel-owl-book" d="M19 96h26v24H19Zm26 0h5v24h-5Z" />
          <path className="pixel-owl-book-mark" d="M25 102h13m-13 5h9" />
          <path className="pixel-owl-face" d="M26 43v-9h8v-8h8v-8h8v-5h16v5h8v8h8v8h8v9l7 7v32l-7 7v8H79v6H49v-6H34v-8l-8-7V50Z" />
          <path className="pixel-owl-face-shadow" d="M27 77h8v10h9v7h36v-7h10V77l7 6v8l-8 7H79v6H49v-6H34l-8-7Z" />
          <path className="pixel-owl-feather pixel-owl-feather-left" d="M29 43 17 38l8 12m4-4-10 8" />
          <path className="pixel-owl-feather pixel-owl-feather-right" d="m99 43 12-5-8 12m-4-4 10 8" />
          <path className="pixel-owl-cap-top" d="M16 31 64 8l48 23-48 25Z" />
          <path className="ai-tutor-mascot-cap" d="M31 35h66v12l-8 8H39l-8-8Z" />
          <path className="pixel-owl-cap-band" d="M32 45h64l-8 7H40Z" />
          <path className="pixel-owl-tassel-line" d="M91 25v31" />
          <path className="pixel-owl-tassel" d="M88 52h8v12h-8Z" />
          <path className="pixel-owl-brow" d="M31 52h25m17 0h25" />
          <PixelEye side="left" blinking={blinking} />
          <PixelEye side="right" blinking={blinking} />
          <path className="pixel-owl-beak" d="m60 73 4-5 4 5-4 8Z" />
          <path className="pixel-owl-collar" d="M48 91h32l-8 11H56Z" />
          <path className="pixel-owl-tie" d="m60 94 4 3 4-3 3 14H57Z" />
          <rect className="pixel-owl-medallion" x="60" y="105" width="8" height="8" />
          <path className="pixel-owl-medallion-star" d="m64 106 2 3 2 1-2 1-2 3-2-3-2-1 2-1Z" />
          <path className="pixel-owl-thought" d="M91 39h4v4h-4Zm7-6h3v3h-3Z" />
        </g>
      </g>
    </svg>
  )
}
