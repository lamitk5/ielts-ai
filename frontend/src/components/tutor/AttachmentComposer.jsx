import { Plus } from 'lucide-react'
import { useRef } from 'react'
import { getAttachmentAcceptAttribute, validateAttachmentFile } from '../../features/tutor/attachmentContract'

export function AttachmentComposer({
  onFileSelected,
  onFilesSelected,
  onError,
  disabled = false,
  hasActiveAttachment = false,
  maxFiles = 5,
  className = '',
}) {
  const fileInputRef = useRef(null)

  function handleTriggerClick() {
    if (disabled || (hasActiveAttachment && !onFilesSelected)) return
    fileInputRef.current?.click()
  }

  function handleFileChange(event) {
    const selectedFiles = Array.from(event.target.files || [])
    if (!selectedFiles.length) return

    if (selectedFiles.length > maxFiles) {
      if (fileInputRef.current) fileInputRef.current.value = ''
      onError?.({ code: 'ATTACHMENT_LIMIT_EXCEEDED', message: `Chỉ được đính kèm tối đa ${maxFiles} tệp.` })
      return
    }

    const invalid = selectedFiles.map((file) => validateAttachmentFile(file)).find((result) => !result.valid)
    if (invalid) {
      if (fileInputRef.current) fileInputRef.current.value = ''
      onError?.(invalid.error)
      return
    }

    if (fileInputRef.current) fileInputRef.current.value = ''
    if (onFilesSelected) onFilesSelected(selectedFiles)
    else onFileSelected?.(selectedFiles[0])
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
        multiple
        onChange={handleFileChange}
        disabled={disabled || (hasActiveAttachment && !onFilesSelected)}
      />
      <button
        type="button"
        className="tutor-attachment-trigger-btn"
        aria-label="Thêm tệp đính kèm"
        title="Đính kèm tài liệu hoặc hình ảnh (tối đa 10MB)"
        onClick={handleTriggerClick}
        disabled={disabled || (hasActiveAttachment && !onFilesSelected)}
      >
        <Plus size={16} aria-hidden="true" />
      </button>
    </div>
  )
}

export default AttachmentComposer
