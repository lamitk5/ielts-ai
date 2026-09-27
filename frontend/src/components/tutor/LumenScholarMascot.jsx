import { useEffect, useState } from 'react'

function useIdleLife(prefersReducedMotion) {
  const [isBlinking, setIsBlinking] = useState(false)
  const [idleTilt, setIdleTilt] = useState('center')

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
    if (prefersReducedMotion) {
      return undefined
    }

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

  return { isBlinking, idleTilt }
}

function ScholarEye({ side, blinking }) {
  const cx = side === 'left' ? 58 : 102
  const eyeClass = `ai-tutor-mascot-eye ai-tutor-mascot-eye-${side} lumen-scholar-eye`

  return (
    <g className={eyeClass} data-blink={blinking ? 'closed' : 'open'}>
      <ellipse className="lumen-scholar-eye-socket" cx={cx} cy="78" rx="25" ry="21" />
      <circle className="lumen-scholar-eye-highlight" cx={cx - 7} cy="70" r="4" />
      <circle
        className="ai-tutor-mascot-pupil lumen-scholar-pupil"
        data-testid={`mascot-pupil-${side}`}
        cx={cx}
        cy="78"
        r="11"
        style={{ transform: 'translate(var(--mascot-pupil-x), var(--mascot-pupil-y))' }}
      />
      <circle className="lumen-scholar-eye-glint" cx={cx + 5} cy="73" r="2.5" />
      <path className="lumen-scholar-eye-lid" d={`M${cx - 23} 78 Q${cx} 98 ${cx + 23} 78 Q${cx} 84 ${cx - 23} 78Z`} />
    </g>
  )
}

export default function LumenScholarMascot({ prefersReducedMotion, isBlinking = false }) {
  const { isBlinking: isIdleBlinking, idleTilt } = useIdleLife(prefersReducedMotion)
  const blinking = isBlinking || isIdleBlinking

  return (
    <svg
      className="ai-tutor-mascot ai-tutor-lumen-scholar"
      data-testid="lumen-scholar-mascot"
      data-idle-motion={prefersReducedMotion ? 'disabled' : 'enabled'}
      data-idle-blink={isIdleBlinking ? 'closed' : 'open'}
      data-blink={blinking ? 'closed' : 'open'}
      data-idle-tilt={prefersReducedMotion ? 'center' : idleTilt}
      viewBox="0 0 160 160"
      role="img"
      aria-label="LUMEN Scholar"
      focusable="false"
    >
      <circle className="ai-tutor-mascot-aura" cx="80" cy="80" r="70" />
      <circle className="lumen-scholar-halo" cx="80" cy="76" r="59" />
      <g
        className="ai-tutor-mascot-face"
        style={{ transform: 'translate(var(--mascot-head-x), var(--mascot-head-y)) rotate(var(--mascot-head-rotate))' }}
      >
        <g className="lumen-scholar-expression">
          <path className="lumen-scholar-shoulders" d="M25 155c3-24 20-38 55-42 35 4 52 18 55 42Z" />
          <path className="lumen-scholar-robe" d="M46 127c10 8 22 12 34 12s24-4 34-12l10 28H36Z" />
          <path className="lumen-scholar-collar" d="m62 125 18 17 18-17 8 30H54Z" />
          <path className="lumen-scholar-head" d="M39 52c2-24 19-37 41-37s39 13 41 37l-8 58c-3 17-15 27-33 27s-30-10-33-27Z" />
          <path className="lumen-scholar-ear lumen-scholar-ear-left" d="m43 49-12-22 25 13Z" />
          <path className="lumen-scholar-ear lumen-scholar-ear-right" d="m117 49 12-22-25 13Z" />
          <path className="lumen-scholar-face-plate" d="M43 73c0-22 16-34 37-34s37 12 37 34c0 27-13 48-37 48S43 100 43 73Z" />
          <path className="ai-tutor-mascot-cap" d="M31 48c7-25 26-37 49-37s42 12 49 37l-12 7H43Z" />
          <path className="lumen-scholar-cap-band" d="M39 49h82" />
          <path className="lumen-scholar-cap-tassel" d="M80 12v24m0 0c-7 0-10 4-10 8h20c0-4-3-8-10-8Z" />
          <path className="lumen-scholar-brow lumen-scholar-brow-left" d="M43 62q15-11 30 0" />
          <path className="lumen-scholar-brow lumen-scholar-brow-right" d="M87 62q15-11 30 0" />
          <ScholarEye side="left" blinking={blinking} />
          <ScholarEye side="right" blinking={blinking} />
          <path className="lumen-scholar-beak" d="m72 98 8-7 8 7-8 9Z" />
          <path className="lumen-scholar-smile" d="M69 113q11 8 22 0" />
          <path className="lumen-scholar-book" d="M27 148q25-10 53 0v10H27Zm53 0q28-10 53 0v10H80Z" />
          <path className="lumen-scholar-book-spine" d="M80 148v12" />
          <path className="lumen-scholar-star" d="m128 72 2 5 5 2-5 2-2 5-2-5-5-2 5-2Z" />
        </g>
      </g>
    </svg>
  )
}
