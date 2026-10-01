import Button from '../common/Button'

export function SpeakingTimer({
  secondsLeft = 0,
  mode = 'PREPARATION',
  isRunning = false,
  onStart,
  onPause,
  onReset,
}) {
  const boundedSeconds = Math.max(0, secondsLeft)
  const minutes = Math.floor(boundedSeconds / 60)
  const seconds = boundedSeconds % 60
  const formattedTime = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`

  const modeLabel =
    mode === 'PREPARATION'
      ? 'Thời gian chuẩn bị (Part 2)'
      : 'Thời gian luyện nói'

  return (
    <div className="speaking-timer-widget speaking-timer-editorial" role="timer" aria-label={modeLabel} aria-live="off">
      <div className="speaking-timer-header">
        <span className="speaking-timer-kicker">{modeLabel}</span>
        <strong className="speaking-timer-digits font-display">{formattedTime}</strong>
      </div>

      {(onStart || onPause || onReset) ? (
        <div className="speaking-timer-controls">
          {isRunning ? (
            <Button type="button" variant="secondary" size="sm" onClick={onPause}>
              Tạm dừng
            </Button>
          ) : (
            <Button type="button" variant="primary" size="sm" onClick={onStart}>
              Bắt đầu
            </Button>
          )}
          {onReset ? (
            <Button type="button" variant="ghost" size="sm" onClick={onReset}>
              Đặt lại
            </Button>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}

export default SpeakingTimer
