import TutorMessage from './TutorMessage'
import TutorMessageSkeleton from './TutorMessageSkeleton'
import TimeoutRetry from './TimeoutRetry'
import { ASSISTANT_NAME } from '../../features/tutor/assistantIdentity'

export function TutorMessageList({ messages = [], loading = false, error = null, onRetry, onCancel }) {
  return (
    <ul className="tutor-message-list" aria-label={`Tin nhắn của ${ASSISTANT_NAME}`} aria-live="polite">
      {messages.map((message) => (
        <TutorMessage key={message.id} message={message} />
      ))}
      {loading ? (
        <li>
          <TutorMessageSkeleton />
        </li>
      ) : null}
      {error ? (
        <li>
          <TimeoutRetry message={error} onRetry={onRetry} onCancel={onCancel} />
        </li>
      ) : null}
    </ul>
  )
}

export default TutorMessageList
