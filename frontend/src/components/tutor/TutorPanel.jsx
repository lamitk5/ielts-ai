import { Send, X } from 'lucide-react'
import { useState } from 'react'
import Button from '../common/Button'
import TutorMessage from './TutorMessage'
import TutorMessageSkeleton from './TutorMessageSkeleton'

const promptSuggestions = [
  'Giải thích lỗi Writing của tôi',
  'Vì sao đáp án Reading này sai?',
  'Luyện Speaking Part 2',
]

function TutorPanel({ messages, loading, onClose, onSend, inputRef }) {
  const [draft, setDraft] = useState('')

  function submitMessage(event) {
    event.preventDefault()
    const trimmed = draft.trim()
    if (!trimmed || loading) return

    onSend(trimmed)
    setDraft('')
  }

  return (
    <section
      id="tutor-dialog"
      className="tutor-panel"
      role="dialog"
      aria-modal="true"
      aria-labelledby="tutor-dialog-title"
    >
      <header className="tutor-panel-header">
        <div>
          <p className="progress-card-kicker">SẴN SÀNG HỖ TRỢ</p>
          <h2 id="tutor-dialog-title" className="font-display">Trợ giảng AI</h2>
        </div>
        <button className="tutor-close-button" type="button" aria-label="Đóng Trợ giảng AI" onClick={onClose}>
          <X aria-hidden="true" size={19} />
        </button>
      </header>

      <ul className="tutor-message-list" aria-label="Tin nhắn Trợ giảng AI" aria-live="polite">
        {messages.map((message) => (
          <TutorMessage key={message.id} message={message} />
        ))}
        {loading ? <li><TutorMessageSkeleton /></li> : null}
      </ul>

      <div className="tutor-prompt-list" aria-label="Gợi ý cho Trợ giảng AI">
        {promptSuggestions.map((prompt) => (
          <button key={prompt} type="button" onClick={() => setDraft(prompt)}>
            {prompt}
          </button>
        ))}
      </div>

      <form className="tutor-input-form" onSubmit={submitMessage}>
        <label className="sr-only" htmlFor="tutor-input">Tin nhắn cho Trợ giảng AI</label>
        <input
          id="tutor-input"
          ref={inputRef}
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="Hỏi về bài luyện của bạn..."
          disabled={loading}
        />
        <Button type="submit" size="sm" aria-label="Gửi câu hỏi" disabled={loading || !draft.trim()}>
          <Send aria-hidden="true" size={16} />
        </Button>
      </form>
    </section>
  )
}

export default TutorPanel
