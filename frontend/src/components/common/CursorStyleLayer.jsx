import { useEffect, useRef } from 'react'
import { useOptionalPreferences } from '../../features/preferences/PreferenceProvider'

function hasCoarsePointer() {
  return window.matchMedia?.('(pointer: coarse)').matches ?? false
}

export default function CursorStyleLayer() {
  const preferences = useOptionalPreferences()
  const layerRef = useRef(null)
  const effectsEnabled = preferences?.preferences?.cursorEffects !== false && !preferences?.reducedMotion

  useEffect(() => {
    const root = document.documentElement
    const coarsePointer = hasCoarsePointer()
    root.dataset.cursorPointer = coarsePointer ? 'coarse' : 'fine'
    if (coarsePointer || !effectsEnabled) return undefined

    const layer = layerRef.current
    let frame = 0
    let latestPoint = null
    const requestFrame = window.requestAnimationFrame ?? ((callback) => window.setTimeout(callback, 0))
    const cancelFrame = window.cancelAnimationFrame ?? window.clearTimeout
    const paint = () => {
      frame = 0
      if (!layer || !latestPoint) return
      layer.style.setProperty('--cursor-x', `${latestPoint.x}px`)
      layer.style.setProperty('--cursor-y', `${latestPoint.y}px`)
      layer.style.setProperty('--cursor-opacity', latestPoint.visible ? '1' : '0')
    }
    const schedulePaint = () => {
      if (!frame) frame = requestFrame(paint)
    }
    const onPointerMove = (event) => {
      if (event.pointerType && event.pointerType !== 'mouse' && event.pointerType !== 'pen') return
      latestPoint = { x: event.clientX, y: event.clientY, visible: true }
      schedulePaint()
    }
    const onPointerOut = (event) => {
      if (event.relatedTarget) return
      latestPoint = { ...(latestPoint ?? { x: 0, y: 0 }), visible: false }
      schedulePaint()
    }

    window.addEventListener('pointermove', onPointerMove, { passive: true })
    window.addEventListener('pointerout', onPointerOut, { passive: true })
    return () => {
      window.removeEventListener('pointermove', onPointerMove)
      window.removeEventListener('pointerout', onPointerOut)
      if (frame) cancelFrame(frame)
    }
  }, [effectsEnabled])

  return <div ref={layerRef} className="cursor-effect-layer" data-testid="cursor-effect-layer" data-active={effectsEnabled && !hasCoarsePointer() ? 'true' : 'false'} aria-hidden="true" />
}
