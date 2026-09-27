import { Maximize2, Minimize2, X } from 'lucide-react'
import { useEffect } from 'react'
import { ASSISTANT_NAME } from '../../features/tutor/assistantIdentity'
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

const TRUSTED_SKILLS = new Set(['READING', 'LISTENING', 'WRITING', 'SPEAKING'])

function getTrustedContext(context) {
  if (!context || !TRUSTED_SKILLS.has(String(context.skill || '').toUpperCase())) {
    return null
  }

  return {
    skill: String(context.skill).toUpperCase(),
    taskType: typeof context.taskType === 'string' ? context.taskType.trim().slice(0, 120) : '',
    exerciseId: typeof context.exerciseId === 'string' ? context.exerciseId.trim().slice(0, 80) : '',
  }
}

function getFullscreenState() {
  return window.matchMedia?.('(max-width: 767px)').matches
    ? SHELL_STATES.FULLSCREEN_MOBILE
    : SHELL_STATES.FULLSCREEN_DESKTOP
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
  attachment,
  onAttachmentSelected,
  onAttachmentError,
  onRemoveAttachment,
  onRetryAttachment,
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
  const trustedContext = getTrustedContext(context)

  const shellClasses = [
    'tutor-panel',
    'tutor-shell',
    'tutor-shell-editorial',
    'tutor-shell-viewport-safe',
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
      aria-label={ASSISTANT_NAME}
      aria-busy={loading}
    >
      <header className="tutor-panel-header">
        <div>
          <p className="progress-card-kicker">SẴN SÀNG HỖ TRỢ</p>
          <h2 id="tutor-dialog-title" className="font-display">
            {ASSISTANT_NAME}
          </h2>
        </div>
        <div className="tutor-header-actions">
          {onStateChange ? (
            <button
              className="tutor-control-button"
              type="button"
              aria-label={isExpanded || isFullscreen ? 'Thu nhỏ' : 'Toàn màn hình'}
              onClick={() =>
                onStateChange(
                  isExpanded || isFullscreen ? SHELL_STATES.STANDARD : getFullscreenState(),
                )
              }
            >
              {isExpanded || isFullscreen ? (
                <Minimize2 aria-hidden="true" size={17} />
              ) : (
                <Maximize2 aria-hidden="true" size={17} />
              )}
            </button>
          ) : null}
          <button
            className="tutor-close-button"
            type="button"
            aria-label={`Đóng ${ASSISTANT_NAME}`}
            onClick={onClose}
          >
            <X aria-hidden="true" size={19} />
          </button>
        </div>
      </header>

      {context ? <ContextBadge context={context} onClearContext={onClearContext} /> : null}

      {isFullscreen && trustedContext ? (
        <aside className="tutor-context-pane" aria-label="Ngữ cảnh bài luyện">
          <p className="tutor-context-pane-kicker">NGỮ CẢNH BÀI LUYỆN</p>
          <div className="tutor-context-pane-values">
            <span className="tutor-context-pane-skill">{trustedContext.skill}</span>
            {trustedContext.taskType ? <span>{trustedContext.taskType}</span> : null}
            {trustedContext.exerciseId ? <span>Mã bài: {trustedContext.exerciseId}</span> : null}
          </div>
        </aside>
      ) : null}

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
            attachment={attachment}
            onAttachmentSelected={onAttachmentSelected}
            onAttachmentError={onAttachmentError}
            onRemoveAttachment={onRemoveAttachment}
            onRetryAttachment={onRetryAttachment}
          />
        </>
      ) : null}
    </section>
  )
}

export default TutorShell
