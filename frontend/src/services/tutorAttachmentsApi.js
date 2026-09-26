import { getStoredSession } from './authApi'
import {
  ATTACHMENT_LIMITS,
  ATTACHMENT_STATUS,
  validateAttachmentFile,
} from '../features/tutor/attachmentContract'

export { ATTACHMENT_LIMITS, ATTACHMENT_STATUS, validateAttachmentFile }

export class AttachmentApiError extends Error {
  constructor(code, message, status) {
    super(message)
    this.name = 'AttachmentApiError'
    this.code = code
    this.status = status
  }
}

function getAuthHeader() {
  const session = getStoredSession()
  return session?.token ? { Authorization: `Bearer ${session.token}` } : {}
}

export async function uploadAttachment(file, options = {}) {
  const validation = validateAttachmentFile(file)
  if (!validation.valid) {
    throw new AttachmentApiError(validation.error.code, validation.error.message, 400)
  }

  const formData = new FormData()
  formData.append('file', file)
  if (options.requestId) formData.append('requestId', String(options.requestId).slice(0, 96))

  const headers = {
    ...getAuthHeader(),
    ...options.headers,
  }

  const response = await fetch('/api/ai/attachments', {
    method: 'POST',
    headers,
    body: formData,
    signal: options.signal,
  })

  const payload = await response.json().catch(() => null)
  if (!response.ok) {
    const code = payload?.error?.code ?? (response.status === 401 ? 'AUTH_REQUIRED' : 'ATTACHMENT_UPLOAD_FAILED')
    const message = payload?.error?.message ?? 'Tải tệp đính kèm thất bại.'
    throw new AttachmentApiError(code, message, response.status)
  }

  return payload
}

export async function getAttachment(id, options = {}) {
  const headers = {
    ...getAuthHeader(),
    ...options.headers,
  }

  const response = await fetch(`/api/ai/attachments/${id}`, {
    method: 'GET',
    headers,
    signal: options.signal,
  })

  const payload = await response.json().catch(() => null)
  if (!response.ok) {
    const code = payload?.error?.code ?? 'ATTACHMENT_FETCH_FAILED'
    const message = payload?.error?.message ?? 'Không thể tải thông tin tệp.'
    throw new AttachmentApiError(code, message, response.status)
  }

  return payload
}

export async function deleteAttachment(id, options = {}) {
  const headers = {
    ...getAuthHeader(),
    ...options.headers,
  }

  const response = await fetch(`/api/ai/attachments/${id}`, {
    method: 'DELETE',
    headers,
    signal: options.signal,
  })

  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    const code = payload?.error?.code ?? 'ATTACHMENT_DELETE_FAILED'
    const message = payload?.error?.message ?? 'Không thể xóa tệp đính kèm.'
    throw new AttachmentApiError(code, message, response.status)
  }
}
