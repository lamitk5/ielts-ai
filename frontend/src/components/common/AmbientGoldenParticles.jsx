import { useEffect, useRef, useState } from 'react'
import { useOptionalPreferences } from '../../features/preferences/PreferenceProvider'

const PARTICLES = [
  { left: 10, top: 12, size: 3, opacity: 0.34, blur: 0, delay: '-2s', duration: '14s', driftX: '10px', driftY: '-12px' },
  { left: 24, top: 24, size: 2, opacity: 0.24, blur: 1, delay: '-7s', duration: '18s', driftX: '-12px', driftY: '8px' },
  { left: 42, top: 9, size: 2, opacity: 0.3, blur: 0, delay: '-10s', duration: '16s', driftX: '8px', driftY: '10px' },
  { left: 59, top: 18, size: 4, opacity: 0.18, blur: 3, delay: '-4s', duration: '21s', driftX: '-14px', driftY: '-10px' },
  { left: 78, top: 11, size: 2, opacity: 0.28, blur: 0, delay: '-12s', duration: '17s', driftX: '12px', driftY: '6px' },
  { left: 91, top: 29, size: 3, opacity: 0.2, blur: 2, delay: '-6s', duration: '20s', driftX: '-9px', driftY: '13px' },
  { left: 16, top: 48, size: 2, opacity: 0.2, blur: 1, delay: '-14s', duration: '19s', driftX: '13px', driftY: '7px' },
  { left: 34, top: 62, size: 3, opacity: 0.26, blur: 0, delay: '-1s', duration: '15s', driftX: '-7px', driftY: '-12px' },
  { left: 52, top: 42, size: 2, opacity: 0.22, blur: 1, delay: '-8s', duration: '22s', driftX: '10px', driftY: '9px' },
  { left: 69, top: 55, size: 4, opacity: 0.16, blur: 3, delay: '-16s', duration: '23s', driftX: '-12px', driftY: '-8px' },
  { left: 86, top: 68, size: 2, opacity: 0.24, blur: 0, delay: '-5s', duration: '18s', driftX: '7px', driftY: '11px' },
  { left: 7, top: 79, size: 3, opacity: 0.18, blur: 2, delay: '-11s', duration: '20s', driftX: '-11px', driftY: '8px' },
  { left: 47, top: 84, size: 2, opacity: 0.28, blur: 0, delay: '-3s', duration: '16s', driftX: '9px', driftY: '-9px' },
  { left: 76, top: 91, size: 3, opacity: 0.2, blur: 1, delay: '-9s', duration: '21s', driftX: '-8px', driftY: '-11px' },
]

function AmbientGoldenParticles() {
  const preferenceContext = useOptionalPreferences()
  const [systemReducedMotion, setSystemReducedMotion] = useState(
    () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false,
  )
  const [pointerMode, setPointerMode] = useState('idle')
  const particleRefs = useRef([])
  const frameRef = useRef(null)
  const cancelFrameRef = useRef(null)
  const reducedMotion = preferenceContext?.reducedMotion ?? systemReducedMotion

  useEffect(() => {
    const motionQuery = window.matchMedia?.('(prefers-reduced-motion: reduce)')
    if (!motionQuery) return undefined
    const handleMotionChange = (event) => setSystemReducedMotion(event.matches)
    motionQuery.addEventListener?.('change', handleMotionChange)
    return () => motionQuery.removeEventListener?.('change', handleMotionChange)
  }, [])

  useEffect(() => {
    if (reducedMotion) return undefined

    const scheduleUpdate = () => {
      if (frameRef.current !== null) return
      if (window.requestAnimationFrame) {
        frameRef.current = window.requestAnimationFrame(() => {
          frameRef.current = null
          cancelFrameRef.current = null
          particleRefs.current.forEach((particle) => {
            if (!particle) return
            const target = particle.dataset.repelTarget?.split(',').map(Number) ?? [0, 0]
            particle.style.setProperty('--repel-x', `${target[0] ?? 0}px`)
            particle.style.setProperty('--repel-y', `${target[1] ?? 0}px`)
          })
        })
        cancelFrameRef.current = () => window.cancelAnimationFrame(frameRef.current)
      } else {
        frameRef.current = window.setTimeout(() => {
          frameRef.current = null
          cancelFrameRef.current = null
          particleRefs.current.forEach((particle) => {
            if (!particle) return
            const target = particle.dataset.repelTarget?.split(',').map(Number) ?? [0, 0]
            particle.style.setProperty('--repel-x', `${target[0] ?? 0}px`)
            particle.style.setProperty('--repel-y', `${target[1] ?? 0}px`)
          })
        }, 16)
        cancelFrameRef.current = () => window.clearTimeout(frameRef.current)
      }
    }

    const resetRepulsion = () => {
      particleRefs.current.forEach((particle) => {
        if (particle) particle.dataset.repelTarget = '0,0'
      })
      setPointerMode('idle')
      scheduleUpdate()
    }

    const handlePointerMove = (event) => {
      if (event.pointerType !== 'mouse') {
        resetRepulsion()
        setPointerMode('disabled')
        return
      }

      const radius = 180
      particleRefs.current.forEach((particle, index) => {
        const item = PARTICLES[index]
        const x = window.innerWidth * item.left / 100
        const y = window.innerHeight * item.top / 100
        const distanceX = event.clientX - x
        const distanceY = event.clientY - y
        const distance = Math.hypot(distanceX, distanceY)
        if (distance === 0 || distance >= radius) {
          particle.dataset.repelTarget = '0,0'
          return
        }
        const force = 1 - distance / radius
        particle.dataset.repelTarget = `${(-distanceX / distance * force * 18).toFixed(2)},${(-distanceY / distance * force * 18).toFixed(2)}`
      })
      setPointerMode('fine')
      scheduleUpdate()
    }

    const handlePointerOut = (event) => {
      if (!event.relatedTarget) resetRepulsion()
    }

    window.addEventListener('pointermove', handlePointerMove, { passive: true })
    window.addEventListener('pointerout', handlePointerOut, { passive: true })
    return () => {
      window.removeEventListener('pointermove', handlePointerMove)
      window.removeEventListener('pointerout', handlePointerOut)
      cancelFrameRef.current?.()
      frameRef.current = null
      cancelFrameRef.current = null
    }
  }, [reducedMotion])

  return (
    <div
      className="ambient-golden-particles"
      data-testid="ambient-golden-particles"
      data-animation-state={reducedMotion ? 'static' : 'animated'}
      data-pointer-mode={reducedMotion ? 'disabled' : pointerMode}
      aria-hidden="true"
      style={{ pointerEvents: 'none' }}
    >
      {PARTICLES.map((particle, index) => (
        <span
          className="ambient-golden-particle"
          key={`${particle.left}-${particle.top}`}
          ref={(node) => { particleRefs.current[index] = node }}
          data-repel-target="0,0"
          style={{
            '--particle-left': `${particle.left}%`,
            '--particle-top': `${particle.top}%`,
            '--particle-size': `${particle.size}px`,
            '--particle-opacity': particle.opacity,
            '--particle-blur': `${particle.blur}px`,
            '--particle-delay': particle.delay,
            '--particle-duration': particle.duration,
            '--particle-drift-x': particle.driftX,
            '--particle-drift-y': particle.driftY,
            '--repel-x': '0px',
            '--repel-y': '0px',
          }}
        />
      ))}
    </div>
  )
}

export default AmbientGoldenParticles
