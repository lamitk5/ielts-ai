const imageExtensions = ['.png', '.jpg', '.jpeg', '.webp']
const imageMimeTypes = ['image/png', 'image/jpeg', 'image/webp']
const documentExtensions = ['.pdf', '.docx', '.txt']
const documentMimeTypes = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/plain',
]

export const ATTACHMENT_CONTRACT = Object.freeze({
  maxSizeBytes: 10 * 1024 * 1024,
  extensions: Object.freeze([...documentExtensions, ...imageExtensions]),
  mimeTypes: Object.freeze([...documentMimeTypes, ...imageMimeTypes]),
  accept: [...documentExtensions, ...imageExtensions, ...documentMimeTypes, ...imageMimeTypes].join(','),
  imageExtensions: Object.freeze(imageExtensions),
  imageMimeTypes: Object.freeze(imageMimeTypes),
})

export const ATTACHMENT_LIMITS = Object.freeze({
  MAX_SIZE_BYTES: ATTACHMENT_CONTRACT.maxSizeBytes,
  ALLOWED_EXTENSIONS: ATTACHMENT_CONTRACT.extensions,
  ALLOWED_MIME_TYPES: ATTACHMENT_CONTRACT.mimeTypes,
  DISALLOWED_EXTENSIONS: ['.html', '.htm', '.svg', '.exe', '.sh', '.bat', '.cmd', '.js', '.mjs', '.py', '.vbs', '.php'],
})

export const ATTACHMENT_STATUS = Object.freeze({
  SELECTED: 'SELECTED',
  UPLOADING: 'UPLOADING',
  UPLOADED: 'UPLOADED',
  PROCESSING: 'PROCESSING',
  READY: 'READY',
  IMAGE_READY: 'IMAGE_READY',
  FAILED: 'FAILED',
  REMOVED: 'REMOVED',
  EXPIRED: 'EXPIRED',
})

const unsupportedMessage = 'Định dạng tệp không được hỗ trợ. Chỉ chấp nhận PDF, DOCX, TXT hoặc PNG/JPG/WEBP.'

export function validateAttachmentFile(file) {
  if (!file) return { valid: false, error: { code: 'ATTACHMENT_EMPTY', message: 'Vui lòng chọn một tệp.' } }
  if (file.size === 0) return { valid: false, error: { code: 'ATTACHMENT_EMPTY', message: 'Tệp đính kèm không có nội dung.' } }
  if (file.size > ATTACHMENT_CONTRACT.maxSizeBytes) {
    return { valid: false, error: { code: 'ATTACHMENT_SIZE_EXCEEDED', message: 'Dung lượng tệp vượt quá giới hạn 10MB.' } }
  }

  const name = String(file.name || '').toLowerCase()
  if (ATTACHMENT_LIMITS.DISALLOWED_EXTENSIONS.some((ext) => name.endsWith(ext))) {
    return { valid: false, error: { code: 'ATTACHMENT_TYPE_NOT_SUPPORTED', message: unsupportedMessage } }
  }
  if (!ATTACHMENT_CONTRACT.extensions.some((ext) => name.endsWith(ext))) {
    return { valid: false, error: { code: 'ATTACHMENT_TYPE_NOT_SUPPORTED', message: unsupportedMessage } }
  }
  return { valid: true, error: null }
}

function extensionFor(filename = '') {
  const lowerName = String(filename).toLowerCase()
  return ATTACHMENT_CONTRACT.extensions.find((extension) => lowerName.endsWith(extension)) || ''
}

export function getAttachmentPresentation(metadata = {}) {
  const contentType = String(metadata.contentType || metadata.mimeType || '').toLowerCase()
  const extension = extensionFor(metadata.filename)
  const isImage = imageMimeTypes.includes(contentType) || imageExtensions.includes(extension)
  return isImage
    ? { kind: 'image', icon: 'image', showThumbnail: true, capability: 'VISION_NOT_ENABLED' }
    : { kind: 'document', icon: 'file-text', showThumbnail: false, capability: null }
}

export function getAttachmentAcceptAttribute() {
  return ATTACHMENT_CONTRACT.accept
}
