export function normalizeAttachmentSource(source = {}) {
  const id = typeof source.id === 'string'
    ? source.id
    : (typeof source.attachmentId === 'string' ? source.attachmentId : '')
  const filename = typeof source.filename === 'string'
    ? source.filename
    : (typeof source.name === 'string' ? source.name : '')
  const kind = typeof source.kind === 'string'
    ? source.kind.toUpperCase()
    : (typeof source.attachmentKind === 'string' ? source.attachmentKind.toUpperCase() : '')
  const status = typeof source.status === 'string' ? source.status.toUpperCase() : ''
  const sizeBytes = Number.isFinite(source.sizeBytes) ? source.sizeBytes : 0
  return { id, filename, kind, status, sizeBytes, preview: filename }
}

export function normalizeAttachmentSources(sources) {
  if (!Array.isArray(sources)) return []
  return sources.map(normalizeAttachmentSource).filter((source) => source.id && source.filename)
}

export function normalizeAttachmentMetadata(metadata) {
  return normalizeAttachmentSource(metadata)
}
