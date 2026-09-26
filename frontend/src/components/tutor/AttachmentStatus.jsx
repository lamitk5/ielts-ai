import { AlertCircle, FileText, Image as ImageIcon, RefreshCw, X } from 'lucide-react'
import { getAttachmentPresentation } from '../../features/tutor/attachmentContract'

function formatFileSize(bytes) {
  if (!bytes || bytes <= 0) return '0 B'
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(0)} KB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

const STATUS_LABELS = {
  SELECTED: 'Đã chọn',
  UPLOADING: 'Đang tải lên...',
  UPLOADED: 'Đã tải lên',
  PROCESSING: 'Đang xử lý...',
  READY: 'Sẵn sàng',
  FAILED: 'Thất bại',
  REMOVED: 'Đã xóa',
  EXPIRED: 'Hết hạn',
}

export function AttachmentStatus({ attachment, onRemove, onRetry, className = '' }) {
  if (!attachment) return null

  const isFailed = attachment.status === 'FAILED'
  const isUploading = attachment.status === 'UPLOADING'
  const isProcessing = attachment.status === 'PROCESSING'
  const formattedSize = formatFileSize(attachment.sizeBytes)
  const statusLabel = STATUS_LABELS[attachment.status] ?? attachment.status
  const presentation = getAttachmentPresentation(attachment)

  return (
    <div
      className={`tutor-attachment-card tutor-attachment-row ${presentation.kind === 'image' ? 'tutor-attachment-image' : 'tutor-attachment-document'} ${isFailed ? 'tutor-attachment-failed' : ''} ${className}`.trim()}
      role="status"
      aria-label={`Tệp đính kèm: ${attachment.filename}`}
    >
      <div className="tutor-attachment-header">
        {attachment.previewUrl ? (
          <img
            src={attachment.previewUrl}
            alt={attachment.filename}
            className="tutor-attachment-thumbnail"
            style={{ width: '2rem', height: '2rem', objectFit: 'cover', borderRadius: '0.35rem' }}
          />
        ) : (
          {presentation.kind === 'image' ? (
            <ImageIcon size={16} aria-hidden="true" className="tutor-attachment-icon" />
          ) : (
            <FileText size={16} aria-hidden="true" className="tutor-attachment-icon" />
          )}
        )}
        <div className="tutor-attachment-meta">
          <span className="tutor-attachment-filename" title={attachment.filename}>
            {attachment.filename}
          </span>
          <span className="tutor-attachment-size">({formattedSize})</span>
        </div>
        <span className={`tutor-attachment-status-badge status-${attachment.status?.toLowerCase()}`}>
          {statusLabel}
        </span>
        {onRemove ? (
          <button
            type="button"
            className="tutor-attachment-remove-btn"
            aria-label="Xóa tệp đính kèm"
            onClick={onRemove}
          >
            <X size={14} aria-hidden="true" />
          </button>
        ) : null}
      </div>

      {isFailed ? (
        <div className="tutor-attachment-error">
          <AlertCircle size={14} aria-hidden="true" />
          <p>{attachment.errorMessage || 'Tải tệp thất bại. Vui lòng thử lại.'}</p>
          {onRetry ? (
            <button
              type="button"
              className="tutor-attachment-retry-btn"
              aria-label="Thử lại tải tệp"
              onClick={onRetry}
            >
              <RefreshCw size={12} aria-hidden="true" />
              <span>Thử lại</span>
            </button>
          ) : null}
        </div>
      ) : null}

      {isUploading || isProcessing ? (
        <div className="tutor-attachment-progress" aria-hidden="true">
          <div className="tutor-attachment-progress-bar animate-pulse" />
        </div>
      ) : null}
    </div>
  )
}

export default AttachmentStatus
