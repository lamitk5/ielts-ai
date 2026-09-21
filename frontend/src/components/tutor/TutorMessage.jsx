import SourceChip from './SourceChip'
import TutorGroundingBadge from './TutorGroundingBadge'

function TutorMessage({ message }) {
  const isUser = message.role === 'user'

  return (
    <li className={`tutor-message ${isUser ? 'tutor-message-user' : 'tutor-message-assistant'}`.trim()}>
      <p>{message.content}</p>
      {!isUser && message.grounding ? (
        <div className="tutor-message-meta">
          <TutorGroundingBadge
            status={message.grounding.status}
            sourceCount={message.grounding.sourceCount}
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
    </li>
  )
}

export default TutorMessage
