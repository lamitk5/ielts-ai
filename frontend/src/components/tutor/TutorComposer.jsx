import { Send, X } from 'lucide-react'
import { useState } from 'react'
import { ASSISTANT_NAME } from '../../features/tutor/assistantIdentity'
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
  attachments: attachmentCollection = null,
  onAttachmentSelected,
  onAttachmentsSelected,
  onAttachmentError,
  onRemoveAttachment,
  onRetryAttachment,
}) {
  const [draft, setDraft] = useState(initialDraft)
  const attachments = attachmentCollection ?? (attachment ? [attachment] : [])
  const isCollectionMode = Array.isArray(attachmentCollection)

  const isAttachmentInProgress = attachments.some((item) => ['UPLOADING', 'PROCESSING'].includes(item.status))
  const hasUnsendableAttachment = isCollectionMode && attachments.some((item) => item.status !== 'READY')

  function submitMessage(event) {
    event.preventDefault()
    const trimmed = draft.trim()
    if (!trimmed || loading || isAttachmentInProgress) return

    if (isCollectionMode && attachments.length > 0) {
      onSend(trimmed, { attachmentIds: attachments.map((item) => item.id) })
    } else if (attachment && attachment.status === 'READY') {
      onSend(trimmed, { attachmentId: attachment.id })
    } else {
      onSend(trimmed)
    }
    setDraft('')
  }

  const isSendDisabled = loading || !draft.trim() || isAttachmentInProgress || hasUnsendableAttachment

  return (
    <div className="tutor-composer-container tutor-composer-viewport-safe">
      {attachments.map((item) => (
        <AttachmentStatus
          key={item.localId || item.id || item.filename}
          attachment={item}
          onRemove={() => onRemoveAttachment?.(item.localId || item.id)}
          onRetry={() => onRetryAttachment?.(item.localId || item.id)}
        />
      ))}
      <form className="tutor-input-form" onSubmit={submitMessage}>
        <AttachmentComposer
          onFileSelected={onAttachmentSelected}
          onFilesSelected={onAttachmentsSelected}
          onError={onAttachmentError}
          disabled={loading || isAttachmentInProgress}
          hasActiveAttachment={Boolean(attachments.length)}
        />
        <label className="sr-only" htmlFor="tutor-input">
          Tin nhắn cho {ASSISTANT_NAME}
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
