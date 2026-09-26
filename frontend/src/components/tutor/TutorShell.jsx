import { Maximize2, Minimize2, X } from 'lucide-react'
import { useEffect } from 'react'
import ContextBadge from './ContextBadge'
import TutorComposer from './TutorComposer'
import TutorMessageList from './TutorMessageList'
import TutorQuickActions from './TutorQuickActions'

export const SHELL_STATES = {
  CLOSED: 'CLOSED',
  COMPACT: 'COMPACT',
  STANDARD: 'STANDARD',
  EXPANDED: 'EXPANDED',
  FULLSCREEN_DESKTOP: 'FULLSCREEN_DESKTOP',
  FULLSCREEN_MOBILE: 'FULLSCREEN_MOBILE',
}

export function TutorShell({
  state = SHELL_STATES.STANDARD,
  context,
  messages = [],
  loading = false,
  onSend,
  onClose,
  onCancel,
  onRetry,
  onStateChange,
  onClearContext,
  inputRef,
  suggestions,
}) {
  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        event.preventDefault()
        onClose?.()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  const isCompact = state === SHELL_STATES.COMPACT
  const isExpanded = state === SHELL_STATES.EXPANDED
  const isFullscreen = state === SHELL_STATES.FULLSCREEN_DESKTOP || state === SHELL_STATES.FULLSCREEN_MOBILE

  const shellClasses = [
    'tutor-panel',
    'tutor-shell',
    isCompact ? 'tutor-shell-compact' : '',
    isExpanded ? 'tutor-shell-expanded' : '',
    isFullscreen ? 'tutor-shell-fullscreen' : '',
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <section
      id="tutor-dialog"
      className={shellClasses}
      role="dialog"
      aria-modal="true"
      aria-labelledby="tutor-dialog-title"
      aria-label="Trợ giảng AI"
      aria-busy={loading}
    >
      <header className="tutor-panel-header">
        <div>
          <p className="progress-card-kicker">SẴN SÀNG HỖ TRỢ</p>
          <h2 id="tutor-dialog-title" className="font-display">
            Trợ giảng AI
          </h2>
        </div>
        <div className="tutor-header-actions">
          {onStateChange ? (
            <>
              <button
                className="tutor-control-button"
                type="button"
                aria-label="Thu gọn"
                onClick={() => onStateChange(isCompact ? SHELL_STATES.STANDARD : SHELL_STATES.COMPACT)}
              >
                <Minimize2 aria-hidden="true" size={17} />
              </button>
              <button
                className="tutor-control-button"
                type="button"
                aria-label={isExpanded ? 'Thu hẹp' : 'Mở rộng'}
                onClick={() => onStateChange(isExpanded ? SHELL_STATES.STANDARD : SHELL_STATES.EXPANDED)}
              >
                <Maximize2 aria-hidden="true" size={17} />
              </button>
            </>
          ) : null}
          <button
            className="tutor-close-button"
            type="button"
            aria-label="Đóng Trợ giảng AI"
            onClick={onClose}
          >
            <X aria-hidden="true" size={19} />
          </button>
        </div>
      </header>

      {context ? <ContextBadge context={context} onClearContext={onClearContext} /> : null}

      {!isCompact ? (
        <>
          <TutorMessageList
            messages={messages}
            loading={loading}
            onRetry={onRetry}
            onCancel={onCancel}
          />

          <TutorQuickActions
            onSelectPrompt={(prompt) => {
              if (onSend) onSend(prompt)
            }}
            suggestions={suggestions}
          />

          <TutorComposer
            onSend={onSend}
            loading={loading}
            onCancel={onCancel}
            inputRef={inputRef}
          />
        </>
      ) : null}
    </section>
  )
}

export default TutorShell
