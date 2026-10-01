import { Sparkles, XCircle } from 'lucide-react'

export function ContextBadge({ context, onClearContext }) {
  if (
    !context ||
    (!context.exerciseId && !context.taskType && (!context.skill || context.skill === 'GENERAL'))
  ) {
    return null
  }

  const skill = context.skill ? context.skill.toUpperCase() : ''
  const taskType = context.taskType || ''
  const isStale = Boolean(context.isStale)

  return (
    <div className={`tutor-context-badge ${isStale ? 'tutor-context-stale' : ''}`} role="status">
      <div className="tutor-context-info">
        <Sparkles size={14} aria-hidden="true" className="tutor-context-icon" />
        <span className="tutor-context-skill">{skill}</span>
        {taskType ? <span className="tutor-context-task">· {taskType}</span> : null}
        {isStale ? <span className="tutor-context-stale-tag">(Ngữ cảnh cũ)</span> : null}
      </div>
      {onClearContext ? (
        <button
          type="button"
          className="tutor-context-clear-btn"
          aria-label="Hỏi chung"
          onClick={onClearContext}
        >
          <XCircle size={14} aria-hidden="true" />
          <span>Hỏi chung</span>
        </button>
      ) : null}
    </div>
  )
}

export default ContextBadge
