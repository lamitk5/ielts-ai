import SourceChip from './SourceChip'
import TutorGroundingBadge from './TutorGroundingBadge'
import { useCrossHighlight } from '../../features/tutor/CrossHighlightContext'
import { TutorReferenceResolver } from '../../features/tutor/TutorReferenceResolver'

function TutorMessage({ message }) {
  const isUser = message.role === 'user'
  const rawGroundingStatus = message.status === 'INSUFFICIENT_CONTEXT'
    ? 'insufficient_context'
    : message.grounding?.status
  const groundingStatus = typeof rawGroundingStatus === 'string'
    ? rawGroundingStatus.toLowerCase()
    : rawGroundingStatus

  const crossHighlight = useCrossHighlight()

  return (
    <li className={`tutor-message ${isUser ? 'tutor-message-user' : 'tutor-message-assistant'} ${message.isError ? 'tutor-message-error' : ''}`.trim()}>
      <p>{message.content}</p>

      {!isUser && Array.isArray(message.references) && message.references.length > 0 ? (
        <div className="tutor-reference-list" aria-label="Tham chiếu bài học">
          {message.references.map((ref, idx) => {
            const resolution = crossHighlight?.registry
              ? TutorReferenceResolver.resolve(ref, crossHighlight.registry, crossHighlight.workspaceState)
              : { status: 'ACTIVE' }

            const label = ref.label || 'Xem vị trí liên quan'

            if (resolution.status === 'STALE') {
              return (
                <div key={ref.targetId || idx} className="tutor-reference-stale-group">
                  <span className="tutor-reference-stale-badge">
                    Tham chiếu bản nháp cũ {ref.draftVersion != null ? `(v${ref.draftVersion})` : ''}
                  </span>
                  <button
                    type="button"
                    className="tutor-reference-chip is-stale"
                    title="Nội dung bài viết đã thay đổi sau khi phản hồi này được tạo"
                  >
                    <span className="tutor-reference-icon" aria-hidden="true">⏱</span>
                    <span>{label}</span>
                  </button>
                  {crossHighlight?.onReanalyzeDraft ? (
                    <button
                      type="button"
                      className="tutor-reanalyze-button"
                      onClick={crossHighlight.onReanalyzeDraft}
                    >
                      Phân tích lại bản nháp
                    </button>
                  ) : null}
                </div>
              )
            }

            return (
              <button
                key={ref.targetId || idx}
                type="button"
                className="tutor-reference-chip"
                onClick={() => crossHighlight?.onActivate?.(ref)}
                onMouseEnter={() => crossHighlight?.onPreview?.(ref)}
                onMouseLeave={() => crossHighlight?.onClearPreview?.(ref)}
                onFocus={() => crossHighlight?.onPreview?.(ref)}
                onBlur={() => crossHighlight?.onClearPreview?.(ref)}
              >
                <span className="tutor-reference-icon" aria-hidden="true">📌</span>
                <span>{label}</span>
              </button>
            )
          })}
        </div>
      ) : null}

      {!isUser && groundingStatus && groundingStatus !== 'NOT_ENABLED' ? (
        <div className="tutor-message-meta">
          <TutorGroundingBadge
            status={groundingStatus}
            sourceCount={message.grounding?.sourceCount ?? message.citations?.length ?? 0}
          />
          {message.citations?.length ? (
            <div className="tutor-source-list" aria-label="Nguồn tham chiếu">
              {message.citations.map((citation) => (
                <SourceChip key={citation.sourceId} citation={citation} />
              ))}
            </div>
          ) : null}
        </div>
      ) : null}

      {message.isError && message.onRetry ? (
        <button type="button" className="tutor-retry-button" onClick={message.onRetry}>Thử lại</button>
      ) : null}
    </li>
  )
}

export default TutorMessage
