import { Paperclip } from 'lucide-react'
import { useRef } from 'react'
import { validateAttachmentFile } from '../../services/tutorAttachmentsApi'

export function AttachmentComposer({
  onFileSelected,
  onError,
  disabled = false,
  hasActiveAttachment = false,
  className = '',
}) {
  const fileInputRef = useRef(null)

  function handleTriggerClick() {
    if (disabled || hasActiveAttachment) return
    fileInputRef.current?.click()
  }

  function handleFileChange(event) {
    const file = event.target.files?.[0]
    if (!file) return

    const validation = validateAttachmentFile(file)
    if (!validation.valid) {
      if (fileInputRef.current) fileInputRef.current.value = ''
      onError?.(validation.error)
      return
    }

    if (fileInputRef.current) fileInputRef.current.value = ''
    onFileSelected?.(file)
  }

  return (
    <div className={`tutor-attachment-composer ${className}`.trim()}>
      <input
        ref={fileInputRef}
        type="file"
        id="tutor-attachment-file-input"
        className="sr-only"
        aria-label="Chọn tệp tải lên"
        accept=".pdf,.docx,.txt,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain"
        onChange={handleFileChange}
        disabled={disabled || hasActiveAttachment}
      />
      <button
        type="button"
        className="tutor-attachment-trigger-btn"
        aria-label="Đính kèm tài liệu"
        title="Đính kèm tài liệu (PDF, DOCX, TXT tối đa 10MB)"
        onClick={handleTriggerClick}
        disabled={disabled || hasActiveAttachment}
      >
        <Paperclip size={16} aria-hidden="true" />
      </button>
    </div>
  )
}

export default AttachmentComposer
