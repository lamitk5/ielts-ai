import { BrainCircuit, Circle, Sparkles } from 'lucide-react'
import { useEffect, useRef } from 'react'
import GlassCard from '../common/GlassCard'
import ParallaxLayer from '../motion/ParallaxLayer'
import { useOptionalPreferences } from '../../features/preferences/PreferenceProvider'

const skillSignals = ['R', 'L', 'W', 'S']

function HeroVisual() {
  const preferences = useOptionalPreferences()
  const systemReducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  const reducedMotion = preferences
    ? preferences.reducedMotion
    : systemReducedMotion || document.documentElement.dataset.reducedMotion === 'true'
  const frameRef = useRef(null)
  const nextTiltRef = useRef({ x: 0, y: 0 })

  useEffect(() => () => {
    if (frameRef.current !== null) {
      window.cancelAnimationFrame?.(frameRef.current)
    }
  }, [])

  const applyTilt = (element, x, y) => {
    element.style.setProperty('--hero-tilt-x', `${x}deg`)
    element.style.setProperty('--hero-tilt-y', `${y}deg`)
  }

  const resetTilt = (element) => {
    if (frameRef.current !== null) {
      window.cancelAnimationFrame?.(frameRef.current)
      frameRef.current = null
    }
    nextTiltRef.current = { x: 0, y: 0 }
    applyTilt(element, 0, 0)
  }

  const handlePointerMove = (event) => {
    if (reducedMotion || event.pointerType === 'touch') return
    const element = event.currentTarget
    const bounds = element.getBoundingClientRect()
    if (!bounds.width || !bounds.height) return
    const normalizedX = ((event.clientX - bounds.left) / bounds.width) * 2 - 1
    const normalizedY = ((event.clientY - bounds.top) / bounds.height) * 2 - 1
    nextTiltRef.current = {
      x: Math.max(-2.4, Math.min(2.4, normalizedY * -2.4)),
      y: Math.max(-2.4, Math.min(2.4, normalizedX * 2.4)),
    }
    if (frameRef.current !== null) return
    const requestFrame = window.requestAnimationFrame ?? ((callback) => window.setTimeout(callback, 0))
    frameRef.current = requestFrame(() => {
      frameRef.current = null
      applyTilt(element, nextTiltRef.current.x, nextTiltRef.current.y)
    })
  }

  return (
    <div
      className={`hero-visual hero-visual-large ${reducedMotion ? 'hero-visual-motion-disabled' : 'hero-visual-motion-enabled'}`}
      data-testid="hero-visual"
      data-hero-visual="learning-intelligence"
      data-motion-state={reducedMotion ? 'static' : 'animated'}
      aria-hidden="true"
    >
      <div className="hero-visual-glow hero-visual-glow-gold" />
      <div className="hero-visual-glow hero-visual-glow-blue" />
      <ParallaxLayer className="hero-orbit hero-orbit-back hero-orbit-motion" distance={12}>
        <span className="hero-orbit-ring hero-orbit-ring-large" />
        <span className="hero-orbit-ring hero-orbit-ring-small" />
      </ParallaxLayer>
      <ParallaxLayer className="hero-orbit hero-orbit-front" distance={8}>
        <span className="hero-orbit-line hero-orbit-line-one" />
        <span className="hero-orbit-line hero-orbit-line-two" />
        <span className="hero-orbit-dot hero-orbit-dot-one" />
        <span className="hero-orbit-dot hero-orbit-dot-two" />
      </ParallaxLayer>
      <GlassCard
        className="hero-intelligence-card"
        onPointerMove={handlePointerMove}
        onPointerLeave={(event) => resetTilt(event.currentTarget)}
      >
        <div className="hero-intelligence-header">
          <span className="hero-intelligence-kicker">Learning intelligence</span>
          <Sparkles aria-hidden="true" size={16} className="hero-intelligence-sparkle" />
        </div>
        <div className="hero-intelligence-core hero-core-motion">
          <span className="hero-core-halo hero-energy-motion" />
          <span className="hero-core-orb hero-core-breathing">
            <BrainCircuit aria-hidden="true" size={30} className="hero-core-brain" />
          </span>
        </div>
        <div className="hero-signal-grid">
          {skillSignals.map((signal) => (
            <div className="hero-signal hero-signal-motion" key={signal}>
              <Circle aria-hidden="true" size={11} />
              <span>{signal}</span>
            </div>
          ))}
        </div>
        <div className="hero-intelligence-footer">
          <span>Én</span>
          <span className="hero-intelligence-status">Context ready</span>
        </div>
      </GlassCard>
    </div>
  )
}

export default HeroVisual
