import { useCallback, useEffect, useRef } from 'react'
import { WORKSPACE_RATIO_PRESETS } from '../../features/preferences/preferenceSchema'

function nearestPreset(value, min, max) {
  const choices = WORKSPACE_RATIO_PRESETS.filter((preset) => preset >= min && preset <= max)
  return choices.reduce((nearest, preset) => Math.abs(preset - value) < Math.abs(nearest - value) ? preset : nearest, choices[0] ?? min)
}

export function WorkspaceDivider({ value = 40, min = 40, max = 60, onChange }) {
  const separatorRef = useRef(null)
  const gridRef = useRef(null)
  const dragRef = useRef(null)
  const boundsRef = useRef(null)
  const frameRef = useRef(null)
  const pointerUpRef = useRef(null)
  const pointerCancelRef = useRef(null)

  const clearTransientRatio = useCallback(() => {
    const grid = gridRef.current
    grid?.style.removeProperty('--workspace-drag-left-ratio')
    grid?.style.removeProperty('--workspace-drag-right-ratio')
    gridRef.current = null
  }, [])

  const queueDragRatio = useCallback((clientX) => {
    const bounds = boundsRef.current
    if (!bounds?.width) return
    const percent = ((clientX - bounds.left) / bounds.width) * 100
    const constrained = Math.min(max, Math.max(min, percent))
    const ratio = nearestPreset(constrained, min, max)
    dragRef.current = ratio
    if (frameRef.current === null) {
      frameRef.current = window.requestAnimationFrame(() => {
        frameRef.current = null
        if (dragRef.current === null) return
        const grid = gridRef.current
        grid?.style.setProperty('--workspace-drag-left-ratio', `${dragRef.current}fr`)
        grid?.style.setProperty('--workspace-drag-right-ratio', `${100 - dragRef.current}fr`)
      })
    }
  }, [max, min])

  const cancelFrame = useCallback(() => {
    if (frameRef.current !== null) window.cancelAnimationFrame(frameRef.current)
    frameRef.current = null
  }, [])

  const onPointerMove = useCallback((event) => queueDragRatio(event.clientX), [queueDragRatio])

  const finishDrag = useCallback((commit) => {
    const ratio = dragRef.current
    cancelFrame()
    dragRef.current = null
    boundsRef.current = null
    clearTransientRatio()
    if (commit && ratio !== null) onChange?.(ratio)
    window.removeEventListener('pointermove', onPointerMove)
    if (pointerUpRef.current) window.removeEventListener('pointerup', pointerUpRef.current)
    if (pointerCancelRef.current) window.removeEventListener('pointercancel', pointerCancelRef.current)
  }, [cancelFrame, clearTransientRatio, onChange, onPointerMove])

  const onPointerUp = useCallback(() => finishDrag(true), [finishDrag])
  const onPointerCancel = useCallback(() => finishDrag(false), [finishDrag])

  const onKeyDown = (event) => {
    const current = nearestPreset(value, min, max)
    const index = WORKSPACE_RATIO_PRESETS.indexOf(current)
    let next
    if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') next = WORKSPACE_RATIO_PRESETS[Math.max(0, index - 1)]
    if (event.key === 'ArrowRight' || event.key === 'ArrowUp') next = WORKSPACE_RATIO_PRESETS[Math.min(WORKSPACE_RATIO_PRESETS.length - 1, index + 1)]
    if (event.key === 'Home') next = min
    if (event.key === 'End') next = max
    if (next !== undefined) {
      event.preventDefault()
      onChange?.(Math.min(max, Math.max(min, next)))
    }
  }

  useEffect(() => {
    pointerUpRef.current = onPointerUp
    pointerCancelRef.current = onPointerCancel
    return () => {
      cancelFrame()
      dragRef.current = null
      boundsRef.current = null
      clearTransientRatio()
      window.removeEventListener('pointermove', onPointerMove)
      window.removeEventListener('pointerup', onPointerUp)
      window.removeEventListener('pointercancel', onPointerCancel)
    }
  }, [cancelFrame, clearTransientRatio, onPointerCancel, onPointerMove, onPointerUp])

  return (
    <div
      ref={separatorRef}
      className="workspace-divider"
      role="separator"
      aria-label="Điều chỉnh độ rộng hai khung"
      aria-orientation="vertical"
      aria-valuemin={min}
      aria-valuemax={max}
      aria-valuenow={value}
      tabIndex={0}
      onKeyDown={onKeyDown}
      onPointerDown={(event) => {
        event.preventDefault()
        event.currentTarget.setPointerCapture?.(event.pointerId)
        gridRef.current = event.currentTarget.parentElement
        boundsRef.current = gridRef.current?.getBoundingClientRect()
        dragRef.current = value
        window.addEventListener('pointermove', onPointerMove)
        window.addEventListener('pointerup', onPointerUp)
        window.addEventListener('pointercancel', onPointerCancel)
      }}
    >
      <span aria-hidden="true" />
    </div>
  )
}
