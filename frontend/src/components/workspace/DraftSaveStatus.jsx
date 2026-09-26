import './workspace.css'

export function DraftSaveStatus({ status = 'idle', error = null }) {
  const configs = {
    idle: { label: 'Bản nháp cục bộ', tone: 'neutral' },
    dirty: { label: 'Chưa lưu', tone: 'warning' },
    saving: { label: 'Đang lưu nháp…', tone: 'info' },
    saved: { label: 'Đã lưu nháp', tone: 'success' },
    save_failed: { label: error ?? 'Lưu nháp thất bại', tone: 'error' },
    error: { label: error ?? 'Lưu nháp thất bại', tone: 'error' },
  }

  const current = configs[status] ?? configs.idle

  return (
    <span
      className={`draft-save-status draft-save-status-${current.tone}`}
      role="status"
      aria-live="polite"
    >
      <span className="draft-save-dot" aria-hidden="true" />
      <span className="draft-save-label">{current.label}</span>
    </span>
  )
}

export default DraftSaveStatus
