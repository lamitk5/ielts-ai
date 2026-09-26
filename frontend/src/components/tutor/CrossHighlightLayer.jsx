import { useEffect, useRef, useState, useCallback } from 'react'
import { TutorReferenceResolver } from '../../features/tutor/TutorReferenceResolver'
import { defaultTutorReferenceRegistry } from '../../features/tutor/TutorReferenceRegistry'
import { CrossHighlightContext, useCrossHighlight } from '../../features/tutor/CrossHighlightContext'

export { CrossHighlightContext, useCrossHighlight }

export function CrossHighlightLayer({
  children,
  registry = defaultTutorReferenceRegistry,
  workspaceState = {},
  onReanalyzeDraft,
}) {
  const [activeTargetId, setActiveTargetId] = useState(null)
  const [previewTargetId, setPreviewTargetId] = useState(null)
  const currentActiveElRef = useRef(null)
  const currentPreviewElRef = useRef(null)

  const clearHighlightAttrs = useCallback(() => {
    if (currentPreviewElRef.current && currentPreviewElRef.current !== currentActiveElRef.current) {
      currentPreviewElRef.current.removeAttribute('data-tutor-highlight')
    }
    if (currentActiveElRef.current) {
      currentActiveElRef.current.removeAttribute('data-tutor-highlight')
      currentActiveElRef.current = null
    }
    setActiveTargetId(null)
    setPreviewTargetId(null)
  }, [])

  const handlePreview = useCallback((reference) => {
    const resolution = TutorReferenceResolver.resolve(reference, registry, workspaceState)
    if (resolution.status !== 'ACTIVE' || !resolution.target?.element) return

    const el = resolution.target.element
    if (el !== currentActiveElRef.current) {
      el.setAttribute('data-tutor-highlight', 'preview')
      currentPreviewElRef.current = el
      setPreviewTargetId(reference.targetId)
    }
  }, [registry, workspaceState])

  const handleClearPreview = useCallback(() => {
    if (currentPreviewElRef.current && currentPreviewElRef.current !== currentActiveElRef.current) {
      currentPreviewElRef.current.removeAttribute('data-tutor-highlight')
      currentPreviewElRef.current = null
      setPreviewTargetId(null)
    }
  }, [])

  const handleActivate = useCallback((reference) => {
    const resolution = TutorReferenceResolver.resolve(reference, registry, workspaceState)
    if (resolution.status !== 'ACTIVE' || !resolution.target?.element) return

    const el = resolution.target.element
    if (currentActiveElRef.current && currentActiveElRef.current !== el) {
      currentActiveElRef.current.removeAttribute('data-tutor-highlight')
    }

    el.setAttribute('data-tutor-highlight', 'active')
    currentActiveElRef.current = el
    setActiveTargetId(reference.targetId)
    setPreviewTargetId(null)

    if (typeof el.scrollIntoView === 'function') {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
    if (typeof el.focus === 'function' && el.tabIndex >= 0) {
      el.focus()
    }
  }, [registry, workspaceState])

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        clearHighlightAttrs()
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => {
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [clearHighlightAttrs])

  const contextValue = {
    registry,
    workspaceState,
    activeTargetId,
    previewTargetId,
    onPreview: handlePreview,
    onClearPreview: handleClearPreview,
    onActivate: handleActivate,
    onClear: clearHighlightAttrs,
    onReanalyzeDraft,
  }

  return (
    <CrossHighlightContext.Provider value={contextValue}>
      <div className="cross-highlight-layer">
        {children}
      </div>
    </CrossHighlightContext.Provider>
  )
}

export default CrossHighlightLayer
