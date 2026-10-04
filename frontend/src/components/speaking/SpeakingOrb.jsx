import { SPEAKING_ROOM_STATES } from '../../features/speaking/speakingRoomState'

const ORB_STATE_DESCRIPTIONS = {
  [SPEAKING_ROOM_STATES.READY]: 'Giám khảo ảo: Sẵn sàng bắt đầu',
  [SPEAKING_ROOM_STATES.PROMPT]: 'Giám khảo ảo: Giám khảo đang đưa ra câu hỏi',
  [SPEAKING_ROOM_STATES.PREPARATION]: 'Giám khảo ảo: Thời gian chuẩn bị câu trả lời',
  [SPEAKING_ROOM_STATES.RECORDING_LOCAL]: 'Giám khảo ảo: Đang ghi âm cục bộ',
  [SPEAKING_ROOM_STATES.TEXT_RESPONSE]: 'Giám khảo ảo: Nhập câu trả lời bằng văn bản',
  [SPEAKING_ROOM_STATES.MIC_PERMISSION_DENIED]: 'Giám khảo ảo: Quyền truy cập microphone bị từ chối',
  [SPEAKING_ROOM_STATES.MIC_UNAVAILABLE]: 'Giám khảo ảo: Microphone không khả dụng',
  [SPEAKING_ROOM_STATES.CANCELLED]: 'Giám khảo ảo: Đã dừng luyện tập',
  [SPEAKING_ROOM_STATES.ERROR]: 'Giám khảo ảo: Lỗi phòng luyện nói',
}

export function SpeakingOrb({ state = SPEAKING_ROOM_STATES.READY, reducedMotion = false }) {
  const description = ORB_STATE_DESCRIPTIONS[state] || 'Giám khảo ảo: Sẵn sàng'

  const stateClass = `speaking-orb-${state.toLowerCase()}`

  return (
    <div
      className={`speaking-orb-wrapper ${stateClass} ${reducedMotion ? 'reduced-motion' : ''}`}
      role="img"
      aria-label={description}
    >
      <div className="speaking-orb-core">
        <div className="speaking-orb-glow" aria-hidden="true" />
        <div className="speaking-orb-inner" aria-hidden="true">
          <span className="speaking-orb-dot" />
        </div>
      </div>
    </div>
  )
}

export default SpeakingOrb
