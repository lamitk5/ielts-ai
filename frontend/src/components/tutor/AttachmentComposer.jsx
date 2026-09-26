import { Plus } from 'lucide-react'
import { useRef } from 'react'
import { getAttachmentAcceptAttribute, validateAttachmentFile } from '../../features/tutor/attachmentContract'

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
        accept={getAttachmentAcceptAttribute()}
        onChange={handleFileChange}
        disabled={disabled || hasActiveAttachment}
      />
      <button
        type="button"
        className="tutor-attachment-trigger-btn"
        aria-label="Thêm tệp đính kèm"
        title="Đính kèm tài liệu hoặc hình ảnh (tối đa 10MB)"
        onClick={handleTriggerClick}
        disabled={disabled || hasActiveAttachment}
      >
        <Plus size={16} aria-hidden="true" />
      </button>
    </div>
  )
}

export default AttachmentComposer
