import { Send, X } from 'lucide-react'
import { useState } from 'react'
import Button from '../common/Button'

export function TutorComposer({ onSend, loading, onCancel, inputRef, initialDraft = '' }) {
  const [draft, setDraft] = useState(initialDraft)

  function submitMessage(event) {
    event.preventDefault()
    const trimmed = draft.trim()
    if (!trimmed || loading) return

    onSend(trimmed)
    setDraft('')
  }

  return (
    <form className="tutor-input-form" onSubmit={submitMessage}>
      <label className="sr-only" htmlFor="tutor-input">
        Tin nhắn cho Trợ giảng AI
      </label>
      <input
        id="tutor-input"
        ref={inputRef}
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
        placeholder="Hỏi về bài luyện của bạn..."
        disabled={loading}
      />
      {loading && onCancel ? (
        <Button
          className="tutor-cancel-action-button"
          type="button"
          size="sm"
          variant="outline"
          aria-label="Hủy"
          onClick={onCancel}
        >
          <X aria-hidden="true" size={16} />
        </Button>
      ) : null}
      <Button
        className="tutor-send-button"
        type="submit"
        size="sm"
        aria-label="Gửi câu hỏi"
        disabled={loading || !draft.trim()}
      >
        <Send aria-hidden="true" size={16} />
      </Button>
    </form>
  )
}

export default TutorComposer
