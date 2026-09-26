import Button from '../common/Button'
import GlassCard from '../common/GlassCard'

export function MicrophonePermissionState({
  permissionState = 'prompt',
  onSwitchToText,
  onRequestPermission,
}) {
  if (permissionState === 'granted' || permissionState === 'prompt') {
    return null
  }

  if (permissionState === 'requesting') {
    return (
      <GlassCard className="speaking-mic-state-card" role="status">
        <p className="speaking-mic-state-text">
          Đang yêu cầu quyền truy cập microphone trên thiết bị của bạn…
        </p>
      </GlassCard>
    )
  }

  const isDenied = permissionState === 'denied'

  return (
    <GlassCard className="speaking-mic-fallback-card" role="alert">
      <p className="speaking-mic-error-text">
        {isDenied
          ? 'Quyền truy cập microphone bị từ chối. Bạn vẫn có thể tiếp tục luyện tập bằng cách ghi lại câu trả lời văn bản.'
          : 'Không tìm thấy thiết bị microphone hợp lệ. Hãy sử dụng chế độ nhập văn bản bên dưới.'}
      </p>
      <div className="speaking-mic-actions">
        {onRequestPermission ? (
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={onRequestPermission}
          >
            Thử lại quyền mic
          </Button>
        ) : null}
        {onSwitchToText ? (
          <Button
            type="button"
            variant="primary"
            size="sm"
            onClick={onSwitchToText}
          >
            Chuyển sang trả lời bằng văn bản
          </Button>
        ) : null}
      </div>
    </GlassCard>
  )
}

export default MicrophonePermissionState
