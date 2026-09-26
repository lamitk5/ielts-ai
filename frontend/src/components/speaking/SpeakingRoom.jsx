import { useState, useReducer, useEffect } from 'react'
import Button from '../common/Button'
import GlassCard from '../common/GlassCard'
import SpeakingOrb from './SpeakingOrb'
import SpeakingPromptCard from './SpeakingPromptCard'
import SpeakingTimer from './SpeakingTimer'
import MicrophonePermissionState from './MicrophonePermissionState'
import LocalAudioVisualizer from './LocalAudioVisualizer'
import { useSpeakingTimer } from '../../features/speaking/useSpeakingTimer'
import { useLocalAudioAmplitude } from '../../features/speaking/useLocalAudioAmplitude'
import {
  SPEAKING_ROOM_STATES,
  speakingRoomReducer,
} from '../../features/speaking/speakingRoomState'
import { useEffectiveReducedMotion } from '../../features/preferences/PreferenceProvider'

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
  const reducedMotion = useEffectiveReducedMotion()

  const isRecording = state.status === SPEAKING_ROOM_STATES.RECORDING_LOCAL

  const {
    permissionState,
    amplitude,
    requestPermission,
  } = useLocalAudioAmplitude({
    isRecording,
    onPermissionChange: (perm) => {
      if (perm === 'denied') {
        dispatch({ type: 'MIC_DENIED' })
      } else if (perm === 'unavailable') {
        dispatch({ type: 'MIC_UNAVAILABLE' })
      }
    },
  })

  const currentPrompt = prompts.find((p) => p.id === selectedPromptId) ?? prompts[0]
  const isPart2 = currentPrompt?.part?.includes('PART 2') || currentPrompt?.part?.includes('Part 2')

  const {
    secondsLeft,
    isRunning,
    start: startTimer,
    pause: pauseTimer,
    reset: resetTimer,
  } = useSpeakingTimer({
    initialDuration: isPart2 ? 60 : 120,
    mode: isPart2 ? 'PREPARATION' : 'PRACTICE',
    onComplete: () => {
      if (isPart2) {
        dispatch({ type: 'START_RECORDING_LOCAL' })
      }
    },
  })

  useEffect(() => {
    resetTimer(isPart2 ? 60 : 120)
  }, [selectedPromptId, isPart2, resetTimer])

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
  }, [initialState, state.status])

  const handleStartRecording = async () => {
    const granted = await requestPermission()
    if (granted) {
      dispatch({ type: 'START_RECORDING_LOCAL' })
      startTimer()
    }
  }

  const handleSubmit = (event) => {
    event?.preventDefault?.()
    onSaveTranscript?.(selectedPromptId, transcript)
  }

  const effectivePermissionState =
    state.status === SPEAKING_ROOM_STATES.MIC_PERMISSION_DENIED
      ? 'denied'
      : state.status === SPEAKING_ROOM_STATES.MIC_UNAVAILABLE
        ? 'unavailable'
        : permissionState

  return (
    <div className="speaking-room speaking-room-editorial speaking-room-single-column-safe" role="region" aria-label="Phòng luyện Speaking">
      <div className="speaking-room-stage speaking-room-stage-editorial" role="region" aria-label="Không gian luyện Speaking">
        <SpeakingOrb state={state.status} reducedMotion={reducedMotion} />

        <div className="speaking-stage-details">
          <SpeakingPromptCard
            prompts={prompts}
            selectedPromptId={selectedPromptId}
            onSelectPrompt={(id) => {
              onSelectPrompt?.(id)
              dispatch({ type: 'START_PROMPT' })
            }}
          />

          <SpeakingTimer
            secondsLeft={secondsLeft}
            mode={isPart2 ? 'PREPARATION' : 'PRACTICE'}
            isRunning={isRunning}
            onStart={startTimer}
            onPause={pauseTimer}
            onReset={() => resetTimer(isPart2 ? 60 : 120)}
          />

          <div className="speaking-local-controls">
            {!isRecording && state.status !== SPEAKING_ROOM_STATES.MIC_PERMISSION_DENIED && state.status !== SPEAKING_ROOM_STATES.MIC_UNAVAILABLE ? (
              <Button
                type="button"
                variant="secondary"
                size="sm"
                onClick={handleStartRecording}
              >
                Bật microphone cục bộ
              </Button>
            ) : null}

            {isRecording ? (
              <Button
                type="button"
                variant="secondary"
                size="sm"
                onClick={() => dispatch({ type: 'SWITCH_TO_TEXT' })}
              >
                Dừng thu âm cục bộ
              </Button>
            ) : null}
          </div>

          <LocalAudioVisualizer
            amplitude={amplitude}
            isRecording={isRecording}
            reducedMotion={reducedMotion}
          />
        </div>
      </div>

      <MicrophonePermissionState
        permissionState={effectivePermissionState}
        onSwitchToText={() => dispatch({ type: 'SWITCH_TO_TEXT' })}
        onRequestPermission={requestPermission}
      />

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
