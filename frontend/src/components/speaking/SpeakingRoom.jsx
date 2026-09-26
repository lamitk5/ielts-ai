import { useState, useReducer, useEffect } from 'react'
import Button from '../common/Button'
import GlassCard from '../common/GlassCard'
import SpeakingOrb from './SpeakingOrb'
import SpeakingPromptCard from './SpeakingPromptCard'
import {
  SPEAKING_ROOM_STATES,
  speakingRoomReducer,
} from '../../features/speaking/speakingRoomState'

export function SpeakingRoom({
  prompts = [],
  selectedPromptId,
  initialState = SPEAKING_ROOM_STATES.READY,
  onSelectPrompt,
  onSaveTranscript,
  submitting = false,
  statusMessage = '',
  errorMessage = '',
}) {
  const [state, dispatch] = useReducer(speakingRoomReducer, { status: initialState, error: null })
  const [transcript, setTranscript] = useState('')

  useEffect(() => {
    if (initialState && initialState !== state.status) {
      if (initialState === SPEAKING_ROOM_STATES.MIC_PERMISSION_DENIED) {
        dispatch({ type: 'MIC_DENIED' })
      } else if (initialState === SPEAKING_ROOM_STATES.MIC_UNAVAILABLE) {
        dispatch({ type: 'MIC_UNAVAILABLE' })
      } else if (initialState === SPEAKING_ROOM_STATES.TEXT_RESPONSE) {
        dispatch({ type: 'SWITCH_TO_TEXT' })
      }
    }
  }, [initialState])

  const handleSubmit = (event) => {
    event?.preventDefault?.()
    onSaveTranscript?.(selectedPromptId, transcript)
  }

  return (
    <div className="speaking-room">
      <div className="speaking-room-stage">
        <SpeakingOrb state={state.status} />

        <SpeakingPromptCard
          prompts={prompts}
          selectedPromptId={selectedPromptId}
          onSelectPrompt={(id) => {
            onSelectPrompt?.(id)
            dispatch({ type: 'START_PROMPT' })
          }}
        />
      </div>

      {state.status === SPEAKING_ROOM_STATES.MIC_PERMISSION_DENIED ||
      state.status === SPEAKING_ROOM_STATES.MIC_UNAVAILABLE ? (
        <GlassCard className="speaking-mic-fallback-card" role="alert">
          <p className="speaking-mic-error-text">
            {state.status === SPEAKING_ROOM_STATES.MIC_PERMISSION_DENIED
              ? 'Quyền truy cập microphone bị từ chối. Bạn vẫn có thể tiếp tục luyện tập bằng cách ghi lại câu trả lời văn bản.'
              : 'Không tìm thấy thiết bị microphone hợp lệ. Hãy sử dụng chế độ nhập văn bản bên dưới.'}
          </p>
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={() => dispatch({ type: 'SWITCH_TO_TEXT' })}
          >
            Chuyển sang trả lời bằng văn bản
          </Button>
        </GlassCard>
      ) : null}

      <form className="speaking-response-form" onSubmit={handleSubmit}>
        <div className="speaking-textarea-group">
          <label htmlFor="speaking-response" className="speaking-response-label">
            Câu trả lời văn bản
          </label>
          <textarea
            id="speaking-response"
            className="speaking-response-textarea"
            value={transcript}
            onChange={(e) => setTranscript(e.target.value)}
            placeholder="Ghi lại ý tưởng hoặc câu trả lời của bạn…"
            rows={5}
          />
        </div>

        <p className="speaking-boundary">
          STT chưa được cấu hình — bạn vẫn có thể lưu input văn bản, không tạo transcript hay band giả.
        </p>

        {errorMessage ? (
          <p className="auth-error" role="alert">
            {errorMessage}
          </p>
        ) : null}

        {statusMessage ? (
          <GlassCard className="speaking-status" role="status">
            Trạng thái: {statusMessage}
          </GlassCard>
        ) : null}

        <div className="speaking-actions">
          <Button type="submit" variant="primary" size="lg" disabled={submitting}>
            {submitting ? 'Đang lưu…' : 'Lưu câu trả lời'}
          </Button>
        </div>
      </form>
    </div>
  )
}

export default SpeakingRoom
