import { Send, X } from 'lucide-react'
import { useState } from 'react'
import Button from '../common/Button'
import AttachmentComposer from './AttachmentComposer'
import AttachmentStatus from './AttachmentStatus'

export function TutorComposer({
  onSend,
  loading = false,
  onCancel,
  inputRef,
  initialDraft = '',
  attachment = null,
  onAttachmentSelected,
  onAttachmentError,
  onRemoveAttachment,
  onRetryAttachment,
}) {
  const [draft, setDraft] = useState(initialDraft)

  const isAttachmentInProgress =
    attachment?.status === 'UPLOADING' || attachment?.status === 'PROCESSING'

  function submitMessage(event) {
    event.preventDefault()
    const trimmed = draft.trim()
    if (!trimmed || loading || isAttachmentInProgress) return

    if (attachment && attachment.status === 'READY') {
      onSend(trimmed, { attachmentId: attachment.id })
    } else {
      onSend(trimmed)
    }
    setDraft('')
  }

  const isSendDisabled = loading || !draft.trim() || isAttachmentInProgress

  return (
    <div className="tutor-composer-container tutor-composer-viewport-safe">
      {attachment ? (
        <AttachmentStatus
          attachment={attachment}
          onRemove={onRemoveAttachment}
          onRetry={onRetryAttachment}
        />
      ) : null}
      <form className="tutor-input-form" onSubmit={submitMessage}>
        <AttachmentComposer
          onFileSelected={onAttachmentSelected}
          onError={onAttachmentError}
          disabled={loading || isAttachmentInProgress}
          hasActiveAttachment={Boolean(attachment)}
        />
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
          disabled={isSendDisabled}
        >
          <Send aria-hidden="true" size={16} />
        </Button>
      </form>
    </div>
  )
}

export default TutorComposer
