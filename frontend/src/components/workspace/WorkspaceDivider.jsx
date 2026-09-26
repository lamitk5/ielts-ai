import { useCallback, useEffect, useRef } from 'react'
import { WORKSPACE_RATIO_PRESETS } from '../../features/preferences/preferenceSchema'

function nearestPreset(value, min, max) {
  const choices = WORKSPACE_RATIO_PRESETS.filter((preset) => preset >= min && preset <= max)
  return choices.reduce((nearest, preset) => Math.abs(preset - value) < Math.abs(nearest - value) ? preset : nearest, choices[0] ?? min)
}

export function WorkspaceDivider({ value = 40, min = 40, max = 60, onChange }) {
  const separatorRef = useRef(null)
  const dragRef = useRef(null)

  const setDragRatio = useCallback((clientX) => {
    const bounds = separatorRef.current?.parentElement?.getBoundingClientRect()
    if (!bounds?.width) return
    const percent = ((clientX - bounds.left) / bounds.width) * 100
    const constrained = Math.min(max, Math.max(min, percent))
    const ratio = nearestPreset(constrained, min, max)
    separatorRef.current?.parentElement?.style.setProperty('--workspace-left-ratio', `${ratio}fr`)
    separatorRef.current?.parentElement?.style.setProperty('--workspace-right-ratio', `${100 - ratio}fr`)
    dragRef.current = ratio
  }, [max, min])

  const onPointerMove = useCallback((event) => setDragRatio(event.clientX), [setDragRatio])
  const finishDrag = useCallback(function finishDragHandler() {
    if (dragRef.current !== null) onChange?.(dragRef.current)
    dragRef.current = null
    window.removeEventListener('pointermove', onPointerMove)
    window.removeEventListener('pointerup', finishDragHandler)
    window.removeEventListener('pointercancel', finishDragHandler)
  }, [onChange, onPointerMove])

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

  useEffect(() => () => {
    window.removeEventListener('pointermove', onPointerMove)
    window.removeEventListener('pointerup', finishDrag)
    window.removeEventListener('pointercancel', finishDrag)
  }, [finishDrag, onPointerMove])

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
        setDragRatio(event.clientX)
        window.addEventListener('pointermove', onPointerMove)
        window.addEventListener('pointerup', finishDrag)
        window.addEventListener('pointercancel', finishDrag)
      }}
    >
      <span aria-hidden="true" />
    </div>
  )
}
