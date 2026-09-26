const ALLOWED_REFERENCE_TYPES = new Set([
  'PASSAGE',
  'QUESTION',
  'OPTION',
  'DRAFT',
  'GENERAL',
  'EXPLANATION',
])

const ALLOWED_SEVERITIES = new Set([
  'INFO',
  'WARNING',
  'ERROR',
  'SUGGESTION',
])

const SAFE_ID_PATTERN = /^[a-zA-Z0-9_-]+$/
const MAX_OFFSET_SPAN = 50_000

export function isValidTutorReference(raw) {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) {
    return false
  }

  const {
    referenceType,
    targetId,
    questionId,
    paragraphId,
    sentenceId,
    startOffset,
    endOffset,
    severity,
    label,
    contentVersion,
    draftVersion,
  } = raw

  if (!referenceType || typeof referenceType !== 'string' || !ALLOWED_REFERENCE_TYPES.has(referenceType.toUpperCase())) {
    return false
  }

  if (!targetId || typeof targetId !== 'string' || !SAFE_ID_PATTERN.test(targetId)) {
    return false
  }

  if (questionId != null && (typeof questionId !== 'string' || !SAFE_ID_PATTERN.test(questionId))) {
    return false
  }

  if (paragraphId != null && (typeof paragraphId !== 'string' || !SAFE_ID_PATTERN.test(paragraphId))) {
    return false
  }

  if (sentenceId != null && (typeof sentenceId !== 'string' || !SAFE_ID_PATTERN.test(sentenceId))) {
    return false
  }

  if (severity != null && (typeof severity !== 'string' || !ALLOWED_SEVERITIES.has(severity.toUpperCase()))) {
    return false
  }

  const hasStart = startOffset != null
  const hasEnd = endOffset != null

  if (hasStart || hasEnd) {
    if (!hasStart || !hasEnd) return false
    if (typeof startOffset !== 'number' || typeof endOffset !== 'number') return false
    if (!Number.isInteger(startOffset) || !Number.isInteger(endOffset)) return false
    if (startOffset < 0 || endOffset < startOffset) return false
    if (endOffset - startOffset > MAX_OFFSET_SPAN) return false
    if (referenceType.toUpperCase() === 'DRAFT' && draftVersion == null && contentVersion == null) {
      return false
    }
  }

  if (label != null) {
    if (typeof label !== 'string' || label.includes('<')) {
      return false
    }
  }

  return true
}

export function normalizeTutorReference(raw) {
  if (!isValidTutorReference(raw)) {
    return null
  }

  return {
    referenceType: raw.referenceType.toUpperCase(),
    targetId: raw.targetId,
    questionId: raw.questionId ?? null,
    paragraphId: raw.paragraphId ?? null,
    sentenceId: raw.sentenceId ?? null,
    startOffset: raw.startOffset ?? null,
    endOffset: raw.endOffset ?? null,
    evidenceId: raw.evidenceId ?? null,
    severity: raw.severity ? raw.severity.toUpperCase() : 'INFO',
    label: raw.label ?? null,
    contentVersion: raw.contentVersion != null ? Number(raw.contentVersion) : null,
    draftVersion: raw.draftVersion != null ? Number(raw.draftVersion) : null,
  }
}

export function normalizeTutorReferences(list) {
  if (!Array.isArray(list)) return []
  return list
    .map(normalizeTutorReference)
    .filter(Boolean)
}
