export const SPEAKING_ROOM_STATES = Object.freeze({
  READY: 'READY',
  PROMPT: 'PROMPT',
  PREPARATION: 'PREPARATION',
  RECORDING_LOCAL: 'RECORDING_LOCAL',
  TEXT_RESPONSE: 'TEXT_RESPONSE',
  MIC_PERMISSION_DENIED: 'MIC_PERMISSION_DENIED',
  MIC_UNAVAILABLE: 'MIC_UNAVAILABLE',
  CANCELLED: 'CANCELLED',
  ERROR: 'ERROR',
})

export const INITIAL_SPEAKING_ROOM_STATE = Object.freeze({
  status: SPEAKING_ROOM_STATES.READY,
  error: null,
})

export function speakingRoomReducer(state = INITIAL_SPEAKING_ROOM_STATE, action) {
  if (!action || typeof action !== 'object') return state

  switch (action.type) {
    case 'START_PROMPT':
      return {
        status: SPEAKING_ROOM_STATES.PROMPT,
        error: null,
      }

    case 'START_PREPARATION':
      return {
        status: SPEAKING_ROOM_STATES.PREPARATION,
        error: null,
      }

    case 'START_RECORDING_LOCAL':
      return {
        status: SPEAKING_ROOM_STATES.RECORDING_LOCAL,
        error: null,
      }

    case 'SWITCH_TO_TEXT':
      return {
        status: SPEAKING_ROOM_STATES.TEXT_RESPONSE,
        error: null,
      }

    case 'MIC_DENIED':
      return {
        status: SPEAKING_ROOM_STATES.MIC_PERMISSION_DENIED,
        error: 'Quyền truy cập microphone bị từ chối.',
      }

    case 'MIC_UNAVAILABLE':
      return {
        status: SPEAKING_ROOM_STATES.MIC_UNAVAILABLE,
        error: 'Không tìm thấy thiết bị microphone hợp lệ.',
      }

    case 'CANCEL':
      return {
        status: SPEAKING_ROOM_STATES.CANCELLED,
        error: null,
      }

    case 'ERROR':
      return {
        status: SPEAKING_ROOM_STATES.ERROR,
        error: action.error || 'Đã có lỗi xảy ra.',
      }

    case 'RESET':
      return { ...INITIAL_SPEAKING_ROOM_STATE }

    default:
      return state
  }
}
