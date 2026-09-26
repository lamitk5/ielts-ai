import { getStoredSession } from './authApi'

export const ATTACHMENT_LIMITS = {
  MAX_SIZE_BYTES: 10 * 1024 * 1024, // 10 MiB
  ALLOWED_EXTENSIONS: ['.pdf', '.docx', '.txt', '.png', '.jpg', '.jpeg', '.webp'],
  ALLOWED_MIME_TYPES: [
    'application/pdf',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'text/plain',
    'image/png',
    'image/jpeg',
    'image/webp',
  ],
  DISALLOWED_EXTENSIONS: [
    '.html', '.htm', '.exe', '.sh', '.bat', '.cmd', '.js', '.mjs', '.py', '.vbs', '.php',
  ],
}

export const ATTACHMENT_STATUS = {
  SELECTED: 'SELECTED',
  UPLOADING: 'UPLOADING',
  UPLOADED: 'UPLOADED',
  PROCESSING: 'PROCESSING',
  READY: 'READY',
  FAILED: 'FAILED',
  REMOVED: 'REMOVED',
  EXPIRED: 'EXPIRED',
}

export class AttachmentApiError extends Error {
  constructor(code, message, status) {
    super(message)
    this.name = 'AttachmentApiError'
    this.code = code
    this.status = status
  }
}

export function validateAttachmentFile(file) {
  if (!file) {
    return {
      valid: false,
      error: { code: 'ATTACHMENT_EMPTY', message: 'Vui lòng chọn một tệp.' },
    }
  }

  if (file.size === 0) {
    return {
      valid: false,
      error: { code: 'ATTACHMENT_EMPTY', message: 'Tệp đính kèm không có nội dung.' },
    }
  }

  if (file.size > ATTACHMENT_LIMITS.MAX_SIZE_BYTES) {
    return {
      valid: false,
      error: {
        code: 'ATTACHMENT_SIZE_EXCEEDED',
        message: 'Dung lượng tệp vượt quá giới hạn 10MB.',
      },
    }
  }

  const name = file.name.toLowerCase()
  for (const ext of ATTACHMENT_LIMITS.DISALLOWED_EXTENSIONS) {
    if (name.endsWith(ext)) {
      return {
        valid: false,
        error: {
          code: 'ATTACHMENT_TYPE_NOT_SUPPORTED',
          message: 'Định dạng tệp không được hỗ trợ. Chỉ chấp nhận PDF, DOCX, TXT hoặc PNG/JPG/WEBP.',
        },
      }
    }
  }

  const isAllowedExt = ATTACHMENT_LIMITS.ALLOWED_EXTENSIONS.some((ext) => name.endsWith(ext))
  if (!isAllowedExt) {
    return {
      valid: false,
      error: {
        code: 'ATTACHMENT_TYPE_NOT_SUPPORTED',
        message: 'Định dạng tệp không được hỗ trợ. Chỉ chấp nhận PDF, DOCX, TXT hoặc PNG/JPG/WEBP.',
      },
    }
  }

  return { valid: true, error: null }
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
