import { describe, expect, test, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
  SPEAKING_ROOM_STATES,
  INITIAL_SPEAKING_ROOM_STATE,
  speakingRoomReducer,
} from '../features/speaking/speakingRoomState'
import { SpeakingRoom } from '../components/speaking/SpeakingRoom'
import { SpeakingOrb } from '../components/speaking/SpeakingOrb'
import { SpeakingPromptCard } from '../components/speaking/SpeakingPromptCard'

describe('Task 1: Speaking Room State Model', () => {
  test('initial state is READY', () => {
    expect(INITIAL_SPEAKING_ROOM_STATE.status).toBe(SPEAKING_ROOM_STATES.READY)
  })

  test('transitions through PROMPT, PREPARATION, RECORDING_LOCAL, and TEXT_RESPONSE', () => {
    let state = speakingRoomReducer(INITIAL_SPEAKING_ROOM_STATE, { type: 'START_PROMPT' })
    expect(state.status).toBe(SPEAKING_ROOM_STATES.PROMPT)

    state = speakingRoomReducer(state, { type: 'START_PREPARATION' })
    expect(state.status).toBe(SPEAKING_ROOM_STATES.PREPARATION)

    state = speakingRoomReducer(state, { type: 'START_RECORDING_LOCAL' })
    expect(state.status).toBe(SPEAKING_ROOM_STATES.RECORDING_LOCAL)

    state = speakingRoomReducer(state, { type: 'SWITCH_TO_TEXT' })
    expect(state.status).toBe(SPEAKING_ROOM_STATES.TEXT_RESPONSE)
  })

  test('handles MIC_PERMISSION_DENIED and MIC_UNAVAILABLE gracefully to text fallback', () => {
    let state = speakingRoomReducer(INITIAL_SPEAKING_ROOM_STATE, { type: 'MIC_DENIED' })
    expect(state.status).toBe(SPEAKING_ROOM_STATES.MIC_PERMISSION_DENIED)

    // Can transition from denied to text response
    state = speakingRoomReducer(state, { type: 'SWITCH_TO_TEXT' })
    expect(state.status).toBe(SPEAKING_ROOM_STATES.TEXT_RESPONSE)

    state = speakingRoomReducer(INITIAL_SPEAKING_ROOM_STATE, { type: 'MIC_UNAVAILABLE' })
    expect(state.status).toBe(SPEAKING_ROOM_STATES.MIC_UNAVAILABLE)
  })

  test('state model does not contain fake STT, pronunciation scoring, or audio transcription states', () => {
    const states = Object.values(SPEAKING_ROOM_STATES)
    for (const s of states) {
      expect(s).not.toMatch(/TRANSCRIBING|PRONUNCIATION_SCORING|AUDIO_UPLOAD|FLUENCY_SCORE/i)
    }
  })
})

describe('Task 1: Speaking Room Components & Text Submission', () => {
  const samplePrompts = [
    { id: 'speaking-p1-01', part: 'PART 1', text: 'Do you enjoy reading in your free time?' },
    { id: 'speaking-p2-01', part: 'PART 2', text: 'Describe a place where you like to study.', points: ['Where it is', 'How often you go there'] },
    { id: 'speaking-p3-01', part: 'PART 3', text: 'How can cities support lifelong learning?' },
  ]

  test('SpeakingPromptCard renders Part 1/2/3 and cue card bullet points', () => {
    render(
      <SpeakingPromptCard
        prompts={samplePrompts}
        selectedPromptId="speaking-p2-01"
        onSelectPrompt={vi.fn()}
      />
    )

    expect(screen.getByText('PART 2')).toBeInTheDocument()
    expect(screen.getByText('Describe a place where you like to study.')).toBeInTheDocument()
    expect(screen.getByText('Where it is')).toBeInTheDocument()
  })

  test('SpeakingOrb renders with accessible examiner state description', () => {
    const { rerender } = render(<SpeakingOrb state={SPEAKING_ROOM_STATES.READY} />)
    expect(screen.getByLabelText(/Sẵn sàng/i)).toBeInTheDocument()

    rerender(<SpeakingOrb state={SPEAKING_ROOM_STATES.PROMPT} />)
    expect(screen.getByLabelText(/Giám khảo đang đưa ra câu hỏi/i)).toBeInTheDocument()

    rerender(<SpeakingOrb state={SPEAKING_ROOM_STATES.RECORDING_LOCAL} />)
    expect(screen.getByLabelText(/Đang ghi âm cục bộ/i)).toBeInTheDocument()
  })

  test('SpeakingRoom provides truthful text input and preserves submission callback', async () => {
    const user = userEvent.setup()
    const onSave = vi.fn()

    render(
      <SpeakingRoom
        prompts={samplePrompts}
        selectedPromptId="speaking-p1-01"
        onSelectPrompt={vi.fn()}
        onSaveTranscript={onSave}
      />
    )

    expect(screen.getByRole('region', { name: 'Phòng luyện Speaking' })).toHaveClass('speaking-room-editorial')
    expect(screen.getByRole('region', { name: 'Không gian luyện Speaking' })).toHaveClass('speaking-room-stage-editorial')
    expect(screen.getByRole('timer')).toHaveClass('speaking-timer-editorial')
    expect(screen.getByRole('img', { name: /sóng âm microphone cục bộ/i })).toHaveClass('local-visualizer-editorial')

    // Truthful boundary note is present
    expect(screen.getByText(/STT chưa được cấu hình/i)).toBeInTheDocument()

    const textarea = screen.getByLabelText(/Câu trả lời văn bản/i)
    await user.type(textarea, 'I really enjoy reading fiction books.')

    const submitBtn = screen.getByRole('button', { name: /Lưu câu trả lời/i })
    await user.click(submitBtn)

    expect(onSave).toHaveBeenCalledWith('speaking-p1-01', 'I really enjoy reading fiction books.')
  })

  test('permission denial message allows direct switch to text path', async () => {
    const user = userEvent.setup()

    render(
      <SpeakingRoom
        prompts={samplePrompts}
        selectedPromptId="speaking-p1-01"
        initialState={SPEAKING_ROOM_STATES.MIC_PERMISSION_DENIED}
        onSelectPrompt={vi.fn()}
        onSaveTranscript={vi.fn()}
      />
    )

    expect(screen.getByText(/Quyền truy cập microphone bị từ chối/i)).toBeInTheDocument()
    const switchBtn = screen.getByRole('button', { name: /Chuyển sang trả lời bằng văn bản/i })
    await user.click(switchBtn)

    expect(screen.getByLabelText(/Câu trả lời văn bản/i)).toBeInTheDocument()
  })
})
