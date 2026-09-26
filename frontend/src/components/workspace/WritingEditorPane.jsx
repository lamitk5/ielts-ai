import { useEffect, useRef } from 'react'
import Button from '../common/Button'
import GlassCard from '../common/GlassCard'
import DraftSaveStatus from './DraftSaveStatus'
import './workspace.css'

export function WritingEditorPane({
  value = '',
  onChange,
  wordCount = 0,
  minWords = 150,
  saveStatus = 'idle',
  timerEnabled = false,
  timerSeconds = 0,
  onToggleTimer,
  error = '',
  assessment = null,
  submitting = false,
  onSubmit,
  registry,
}) {
  const paneRef = useRef(null)

  useEffect(() => {
    if (!registry || !paneRef.current) return
    const unregister = registry.register({
      targetId: 'writing-editor',
      type: 'DRAFT',
      element: paneRef.current,
    })
    return () => {
      try { unregister?.() } catch {}
    }
  }, [registry])

  const formatTimer = (totalSeconds) => {
    const minutes = Math.floor(totalSeconds / 60)
    const seconds = totalSeconds % 60
    return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }

  return (
    <div
      ref={paneRef}
      className="writing-editor-pane writing-editor-pane-editorial"
      data-writing-target-id="writing-editor"
      tabIndex={-1}
    >
      <div className="writing-editor-toolbar writing-editor-action-bar">
        <div className="writing-editor-stats">
          <span className={`writing-word-count-badge ${wordCount >= minWords ? 'is-sufficient' : 'is-insufficient'}`}>
            <strong className="writing-word-count-number">{wordCount} từ</strong>
            <span className="writing-word-count-hint"> (yêu cầu {minWords})</span>
          </span>
          <DraftSaveStatus status={saveStatus} />
        </div>

        <div className="writing-editor-tools">
          <button
            type="button"
            className={`writing-timer-toggle ${timerEnabled ? 'is-active' : ''}`}
            onClick={onToggleTimer}
            aria-label={timerEnabled ? 'Tạm dừng đồng hồ bấm giờ' : 'Bắt đầu đếm giờ luyện tập'}
          >
            <span className="writing-timer-icon" aria-hidden="true">⏱</span>
            <span className="writing-timer-label">
              {timerEnabled ? formatTimer(timerSeconds) : 'Đồng hồ bấm giờ'}
            </span>
          </button>
        </div>
      </div>

      <div className="writing-textarea-wrapper">
        <label htmlFor="writing-response" className="sr-only">
          Bài viết
        </label>
        <textarea
          id="writing-response"
          className="writing-editor-textarea"
          value={value}
          onChange={(e) => onChange?.(e.target.value)}
          placeholder="Bắt đầu viết bài của bạn tại đây…"
          rows={14}
        />
      </div>

      <div className="writing-editor-footer writing-editor-footer-editorial">
        <p className="writing-boundary">
          Band ước lượng sẽ chỉ xuất hiện khi đánh giá AI trả về dữ liệu hợp lệ; đây không phải điểm thi chính thức.
        </p>

        {error ? (
          <p className="auth-error" role="alert">
            {error}
          </p>
        ) : null}

        {assessment ? (
          <GlassCard className="writing-assessment" role="status">
            <div className="writing-assessment-header">
              <span className="writing-assessment-kicker">KẾT QUẢ ĐÁNH GIÁ TỰ ĐỘNG</span>
              {assessment.overallBandEstimate == null ? (
                <strong className="writing-assessment-band font-display">Chưa khả dụng</strong>
              ) : (
                <strong className="writing-assessment-band font-display">
                  Band ước lượng {assessment.overallBandEstimate}
                </strong>
              )}
            </div>
            <p className="writing-assessment-disclaimer">{assessment.disclaimer}</p>
          </GlassCard>
        ) : null}

        <div className="writing-submit-row">
          <Button
            type="submit"
            variant="primary"
            size="lg"
            disabled={submitting}
            onClick={onSubmit}
          >
            {submitting ? 'Đang gửi…' : 'Gửi bài viết'}
          </Button>
        </div>
      </div>
    </div>
  )
}

export default WritingEditorPane
