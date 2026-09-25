import SourceChip from './SourceChip'
import TutorGroundingBadge from './TutorGroundingBadge'

function TutorMessage({ message }) {
  const isUser = message.role === 'user'
  const rawGroundingStatus = message.status === 'INSUFFICIENT_CONTEXT'
    ? 'insufficient_context'
    : message.grounding?.status
  const groundingStatus = typeof rawGroundingStatus === 'string'
    ? rawGroundingStatus.toLowerCase()
    : rawGroundingStatus

  return (
    <li className={`tutor-message ${isUser ? 'tutor-message-user' : 'tutor-message-assistant'} ${message.isError ? 'tutor-message-error' : ''}`.trim()}>
      <p>{message.content}</p>
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
