import { AlertCircle, RefreshCw, X } from 'lucide-react'
import Button from '../common/Button'

export function TimeoutRetry({ message, onRetry, onCancel }) {
  return (
    <div className="tutor-timeout-retry" role="alert">
      <div className="tutor-timeout-header">
        <AlertCircle size={16} aria-hidden="true" className="tutor-timeout-icon" />
        <p className="tutor-timeout-message">{message || 'Không thể kết nối tới Trợ giảng AI. Vui lòng thử lại.'}</p>
      </div>
      <div className="tutor-timeout-actions">
        {onRetry ? (
          <Button
            type="button"
            size="sm"
            className="button-primary tutor-retry-btn"
            onClick={onRetry}
          >
            <RefreshCw size={14} aria-hidden="true" />
            <span>Thử lại</span>
          </Button>
        ) : null}
        {onCancel ? (
          <Button
            type="button"
            size="sm"
            className="button-outline tutor-cancel-btn"
            onClick={onCancel}
          >
            <X size={14} aria-hidden="true" />
            <span>Hủy</span>
          </Button>
        ) : null}
      </div>
    </div>
  )
}

export default TimeoutRetry
