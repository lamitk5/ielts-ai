import { useEffect, useMemo, useRef, useState } from 'react'
import { useOptionalPreferences } from '../../features/preferences/PreferenceProvider'
import { calculateRepulsion, clampVelocity, MAX_REPEL_OFFSET, MAX_REPEL_VELOCITY, REPULSION_RADIUS } from './particlePhysics'

const PARTICLE_BUDGETS = Object.freeze({
  desktop: 64,
  largeTablet: 54,
  tablet: 45,
  mobile: 24,
})

const PARTICLE_LAYERS = Object.freeze([
  { name: 'dust', count: 38 },
  { name: 'glow', count: 19 },
  { name: 'spark', count: 7 },
])

const REGION_GROUPS = Object.freeze({
  dust: [
    { left: 8, top: 12, region: 'hero-margin' },
    { left: 20, top: 22, region: 'hero-margin' },
    { left: 36, top: 10, region: 'hero-margin' },
    { left: 53, top: 16, region: 'negative-space' },
    { left: 72, top: 8, region: 'negative-space' },
    { left: 90, top: 24, region: 'negative-space' },
    { left: 14, top: 42, region: 'hero-text-margin' },
    { left: 30, top: 58, region: 'negative-space' },
    { left: 48, top: 40, region: 'search' },
    { left: 66, top: 54, region: 'negative-space' },
    { left: 84, top: 68, region: 'negative-space' },
    { left: 6, top: 78, region: 'negative-space' },
    { left: 42, top: 82, region: 'negative-space' },
    { left: 74, top: 90, region: 'negative-space' },
  ],
  glow: [
    { left: 18, top: 18, region: 'hero-text-margin' },
    { left: 38, top: 30, region: 'search' },
    { left: 58, top: 26, region: 'search' },
    { left: 78, top: 18, region: 'negative-space' },
    { left: 92, top: 44, region: 'negative-space' },
    { left: 24, top: 54, region: 'hero-text-margin' },
    { left: 46, top: 48, region: 'search' },
    { left: 68, top: 64, region: 'learning-visual' },
    { left: 88, top: 78, region: 'negative-space' },
  ],
  spark: [
    { left: 28, top: 14, region: 'hero-margin' },
    { left: 50, top: 34, region: 'search' },
    { left: 70, top: 28, region: 'search' },
    { left: 86, top: 42, region: 'negative-space' },
    { left: 16, top: 70, region: 'hero-text-margin' },
    { left: 58, top: 76, region: 'learning-visual' },
    { left: 94, top: 88, region: 'negative-space' },
  ],
})

const LAYER_TOKENS = Object.freeze({
  dark: {
    dust: { core: '#d7bf82', halo: 'rgba(229, 201, 130, 0.45)', shadow: '0 0 8px rgba(229, 201, 130, 0.28)' },
    glow: { core: '#e5c982', halo: 'rgba(229, 201, 130, 0.68)', shadow: '0 0 12px rgba(229, 201, 130, 0.62), 0 0 28px rgba(207, 174, 103, 0.28)' },
    spark: { core: '#fff1bd', halo: 'rgba(229, 201, 130, 0.82)', shadow: '0 0 8px rgba(255, 241, 189, 0.9), 0 0 24px rgba(229, 201, 130, 0.58)' },
  },
  light: {
    dust: { core: '#9a6a16', halo: 'rgba(154, 106, 22, 0.34)', shadow: '0 0 7px rgba(154, 106, 22, 0.24)' },
    glow: { core: '#a87520', halo: 'rgba(168, 117, 32, 0.48)', shadow: '0 0 10px rgba(168, 117, 32, 0.38), 0 0 22px rgba(122, 89, 31, 0.2)' },
    spark: { core: '#b47a1e', halo: 'rgba(196, 145, 61, 0.56)', shadow: '0 0 7px rgba(180, 122, 30, 0.56), 0 0 20px rgba(154, 106, 22, 0.32)' },
  },
})

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

function getParticleBudget(width) {
  if (width >= 1280) return PARTICLE_BUDGETS.desktop
  if (width >= 1024) return PARTICLE_BUDGETS.largeTablet
  if (width >= 768) return PARTICLE_BUDGETS.tablet
  return PARTICLE_BUDGETS.mobile
}

function createParticleDefinitions() {
  return PARTICLE_LAYERS.flatMap(({ name, count }) => (
    Array.from({ length: count }, (_, index) => {
      const regionGroup = REGION_GROUPS[name]
      const base = regionGroup[index % regionGroup.length]
      const jitter = ((index * 11) % 7) - 3
      const layerIndex = index + (name === 'dust' ? 0 : name === 'glow' ? 38 : 57)
      const layerConfig = name === 'dust'
        ? { size: 1.4 + (index % 3) * 0.3, opacity: 0.38 + (index % 4) * 0.035, blur: index % 6 === 0 ? 0.4 : 0, duration: 17 + (index % 6), twinkle: false }
        : name === 'glow'
        ? { size: 3 + (index % 3), opacity: 0.5 + (index % 3) * 0.045, blur: 0.7 + (index % 3) * 0.45, duration: 22 + (index % 5), twinkle: true }
        : { size: 2.8 + (index % 3) * 0.35, opacity: 0.68 + (index % 2) * 0.06, blur: 0.2, duration: 19 + (index % 4), twinkle: true }

      return {
        id: `${name}-${layerIndex}`,
        layer: name,
        region: base.region,
        left: clamp(base.left + jitter, 2, 98),
        top: clamp(base.top + ((index * 7) % 5) - 2, 3, 97),
        ...layerConfig,
        delay: `-${2 + ((index * 5) % 17)}s`,
        driftX: `${(index % 2 ? -1 : 1) * (5 + (index % 6))}px`,
        driftY: `${(index % 3 ? 1 : -1) * (5 + ((index + 2) % 6))}px`,
      }
    })
  ))
}

const PARTICLE_DEFINITIONS = Object.freeze(createParticleDefinitions())

function selectParticles(width) {
  const budget = getParticleBudget(width)
  const layerBudgets = {
    dust: Math.round(budget * 0.6),
    glow: Math.round(budget * 0.3),
    spark: budget - Math.round(budget * 0.6) - Math.round(budget * 0.3),
  }

  return Object.entries(layerBudgets).flatMap(([layer, count]) => (
    PARTICLE_DEFINITIONS.filter((particle) => particle.layer === layer).slice(0, count)
  ))
}

function getThemeTokens(theme, layer) {
  return LAYER_TOKENS[theme][layer]
}

function AmbientGoldenParticles() {
  const preferenceContext = useOptionalPreferences()
  const [systemReducedMotion, setSystemReducedMotion] = useState(
    () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false,
  )
  const [pointerMode, setPointerMode] = useState('idle')
  const [viewportWidth, setViewportWidth] = useState(() => window.innerWidth)
  const particleRefs = useRef([])
  const particleMotionRef = useRef([])
  const pointerRef = useRef({ active: false, x: 0, y: 0 })
  const pointerModeRef = useRef('idle')
  const frameRef = useRef(null)
  const particleTheme = preferenceContext?.preferences.themeMode === 'light' || document.documentElement.dataset.theme === 'light' ? 'light' : 'dark'
  const reducedMotion = preferenceContext?.reducedMotion ?? systemReducedMotion
  const particles = useMemo(() => selectParticles(viewportWidth), [viewportWidth])

  useEffect(() => {
    const motionQuery = window.matchMedia?.('(prefers-reduced-motion: reduce)')
    if (!motionQuery) return undefined
    const handleMotionChange = (event) => setSystemReducedMotion(event.matches)
    motionQuery.addEventListener?.('change', handleMotionChange)
    return () => motionQuery.removeEventListener?.('change', handleMotionChange)
  }, [])

  useEffect(() => {
    const handleResize = () => setViewportWidth(window.innerWidth)
    window.addEventListener('resize', handleResize, { passive: true })
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  useEffect(() => {
    particleMotionRef.current = particles.map(() => ({ x: 0, y: 0, vx: 0, vy: 0 }))
  }, [particles])

  useEffect(() => {
    if (reducedMotion) {
      return undefined
    }

    const writeMotion = () => {
      particleRefs.current.forEach((particle, index) => {
        if (!particle) return
        const motion = particleMotionRef.current[index]
        particle.style.setProperty('--repel-x', `${motion?.x ?? 0}px`)
        particle.style.setProperty('--repel-y', `${motion?.y ?? 0}px`)
      })
    }

    const animate = () => {
      frameRef.current = null
      let moving = pointerRef.current.active

      particleRefs.current.forEach((particle, index) => {
        const motion = particleMotionRef.current[index]
        const item = particles[index]
        if (!particle || !motion || !item) return

        const x = window.innerWidth * item.left / 100
        const y = window.innerHeight * item.top / 100
        const target = pointerRef.current.active
          ? calculateRepulsion(pointerRef.current.x - x, pointerRef.current.y - y)
          : { x: 0, y: 0, strength: 0 }
        const [vx, vy] = clampVelocity(
          motion.vx * 0.86 + (target.x - motion.x) * 0.18,
          motion.vy * 0.86 + (target.y - motion.y) * 0.18,
        )
        motion.vx = vx
        motion.vy = vy
        motion.x = clamp(motion.x + vx, -MAX_REPEL_OFFSET, MAX_REPEL_OFFSET)
        motion.y = clamp(motion.y + vy, -MAX_REPEL_OFFSET, MAX_REPEL_OFFSET)
        if (Math.abs(motion.x) > 0.05 || Math.abs(motion.y) > 0.05 || Math.abs(vx) > 0.05 || Math.abs(vy) > 0.05) moving = true
      })

      writeMotion()
      if (moving) frameRef.current = window.requestAnimationFrame(animate)
    }

    const scheduleMotion = () => {
      if (frameRef.current !== null) return
      if (window.requestAnimationFrame) frameRef.current = window.requestAnimationFrame(animate)
    }

    const resetRepulsion = () => {
      pointerRef.current.active = false
      if (pointerModeRef.current !== 'idle') {
        pointerModeRef.current = 'idle'
        setPointerMode('idle')
      }
      scheduleMotion()
    }

    const handlePointerMove = (event) => {
      if (event.pointerType !== 'mouse') {
        resetRepulsion()
        if (pointerModeRef.current !== 'disabled') {
          pointerModeRef.current = 'disabled'
          setPointerMode('disabled')
        }
        return
      }

      pointerRef.current = { active: true, x: event.clientX, y: event.clientY }
      if (pointerModeRef.current !== 'fine') {
        pointerModeRef.current = 'fine'
        setPointerMode('fine')
      }
      scheduleMotion()
    }

    const handlePointerOut = (event) => {
      if (!event.relatedTarget) resetRepulsion()
    }

    window.addEventListener('pointermove', handlePointerMove, { passive: true })
    window.addEventListener('pointerout', handlePointerOut, { passive: true })
    const particleNodes = particleRefs.current
    return () => {
      window.removeEventListener('pointermove', handlePointerMove)
      window.removeEventListener('pointerout', handlePointerOut)
      if (frameRef.current !== null) window.cancelAnimationFrame(frameRef.current)
      frameRef.current = null
      pointerRef.current.active = false
      pointerModeRef.current = 'idle'
      particleMotionRef.current.forEach((motion) => {
        motion.x = 0
        motion.y = 0
        motion.vx = 0
        motion.vy = 0
      })
      particleNodes.forEach((particle) => {
        particle?.style.setProperty('--repel-x', '0px')
        particle?.style.setProperty('--repel-y', '0px')
      })
    }
  }, [particles, reducedMotion])

  return (
    <div
      className="ambient-golden-particles"
      data-testid="ambient-golden-particles"
      data-animation-state={reducedMotion ? 'static' : 'animated'}
      data-pointer-mode={reducedMotion ? 'disabled' : pointerMode}
      data-particle-theme={particleTheme}
      data-particle-count={particles.length}
      data-repulsion-radius={REPULSION_RADIUS}
      data-max-velocity={MAX_REPEL_VELOCITY}
      aria-hidden="true"
      style={{ pointerEvents: 'none' }}
    >
      {particles.map((particle, index) => {
        const tokens = getThemeTokens(particleTheme, particle.layer)
        return (
          <span
            className={`ambient-golden-particle ambient-golden-particle-${particle.layer} ${particleTheme === 'light' ? 'ambient-golden-particle-light' : ''}`.trim()}
            key={particle.id}
            ref={(node) => { particleRefs.current[index] = node }}
            data-particle-layer={particle.layer}
            data-particle-region={particle.region}
            data-repel-target="0,0"
            style={{
              '--particle-left': `${particle.left}%`,
              '--particle-top': `${particle.top}%`,
              '--particle-size': `${particle.size}px`,
              '--particle-opacity': particle.opacity,
              '--particle-blur': `${particle.blur}px`,
              '--particle-delay': particle.delay,
              '--particle-duration': `${particle.duration}s`,
              '--particle-drift-x': particle.driftX,
              '--particle-drift-y': particle.driftY,
              '--particle-core': tokens.core,
              '--particle-halo': tokens.halo,
              '--particle-shadow': tokens.shadow,
              '--repel-x': '0px',
              '--repel-y': '0px',
            }}
          />
        )
      })}
    </div>
  )
}

export default AmbientGoldenParticles
