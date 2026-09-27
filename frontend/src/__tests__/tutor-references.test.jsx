import { describe, expect, test } from 'vitest'
import {
  isValidTutorReference,
  normalizeTutorReference,
  normalizeTutorReferences,
} from '../features/tutor/tutorReferenceSchema'

describe('Tutor reference schema & normalization', () => {
  test('accepts valid immutable Reading passage reference', () => {
    const raw = {
      referenceType: 'PASSAGE',
      targetId: 'reading-foundation-01-p1',
      paragraphId: 'p1',
      severity: 'INFO',
      label: 'Đoạn 1 bài đọc',
    }

    expect(isValidTutorReference(raw)).toBe(true)
    const normalized = normalizeTutorReference(raw)
    expect(normalized).toEqual({
      referenceType: 'PASSAGE',
      targetId: 'reading-foundation-01-p1',
      questionId: null,
      paragraphId: 'p1',
      sentenceId: null,
      startOffset: null,
      endOffset: null,
      evidenceId: null,
      severity: 'INFO',
      label: 'Đoạn 1 bài đọc',
      contentVersion: null,
      draftVersion: null,
    })
  })

  test('accepts valid version-bound Writing draft reference with offsets', () => {
    const raw = {
      referenceType: 'DRAFT',
      targetId: 'writing-editor',
      startOffset: 12,
      endOffset: 58,
      severity: 'SUGGESTION',
      label: 'Lỗi ngữ pháp cần chỉnh',
      draftVersion: 3,
    }

    expect(isValidTutorReference(raw)).toBe(true)
    const normalized = normalizeTutorReference(raw)
    expect(normalized.draftVersion).toBe(3)
    expect(normalized.startOffset).toBe(12)
    expect(normalized.endOffset).toBe(58)
  })

  test('rejects unversioned mutable draft references with offsets', () => {
    const raw = {
      referenceType: 'DRAFT',
      targetId: 'writing-editor',
      startOffset: 10,
      endOffset: 25,
    }

    expect(isValidTutorReference(raw)).toBe(false)
    expect(normalizeTutorReference(raw)).toBeNull()
  })

  test('rejects CSS selectors, XPath, HTML, and arbitrary DOM injections', () => {
    const injections = [
      { referenceType: 'PASSAGE', targetId: 'div.reading > p:nth-child(1)' },
      { referenceType: 'PASSAGE', targetId: '//div[@id="test"]' },
      { referenceType: 'PASSAGE', targetId: '<script>alert(1)</script>' },
      { referenceType: 'PASSAGE', targetId: 'javascript:void(0)' },
      { referenceType: 'PASSAGE', targetId: 'http://evil.com' },
      { referenceType: 'PASSAGE', targetId: '..//etc/passwd' },
    ]

    for (const item of injections) {
      expect(isValidTutorReference(item)).toBe(false)
      expect(normalizeTutorReference(item)).toBeNull()
    }
  })

  test('rejects invalid offsets', () => {
    // Negative offset
    expect(isValidTutorReference({
      referenceType: 'DRAFT',
      targetId: 'writing-editor',
      startOffset: -1,
      endOffset: 10,
      draftVersion: 1,
    })).toBe(false)

    // Inverted offsets
    expect(isValidTutorReference({
      referenceType: 'DRAFT',
      targetId: 'writing-editor',
      startOffset: 50,
      endOffset: 20,
      draftVersion: 1,
    })).toBe(false)

    // Exceeding max offset span (50,000)
    expect(isValidTutorReference({
      referenceType: 'DRAFT',
      targetId: 'writing-editor',
      startOffset: 0,
      endOffset: 50_001,
      draftVersion: 1,
    })).toBe(false)
  })

  test('normalizes an array of references filtering out invalid entries', () => {
    const list = [
      { referenceType: 'PASSAGE', targetId: 'p1', label: 'Valid 1' },
      { referenceType: 'PASSAGE', targetId: '<invalid>' },
      { referenceType: 'QUESTION', targetId: 'q2', label: 'Valid 2' },
      null,
      'invalid string',
    ]

    const result = normalizeTutorReferences(list)
    expect(result).toHaveLength(2)
    expect(result[0].targetId).toBe('p1')
    expect(result[1].targetId).toBe('q2')
  })

  test('sendTutorMessage extracts and normalizes references from backend payload', async () => {
    const { sendTutorMessage } = await import('../services/aiTutorApi')
    const originalFetch = global.fetch
    global.fetch = async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        status: 'ANSWERED',
        answer: 'Hãy xem lại đoạn 1.',
        references: [
          { referenceType: 'PASSAGE', targetId: 'passage-p1', label: 'Đoạn 1' },
          { referenceType: 'DRAFT', targetId: 'writing-editor', startOffset: 0, endOffset: 10, draftVersion: 2 },
          { referenceType: 'DRAFT', targetId: '<script>bad</script>' },
        ],
      }),
    })

    try {
      const response = await sendTutorMessage({ message: 'Giải thích đoạn 1' })
      expect(response.references).toHaveLength(2)
      expect(response.references[0].targetId).toBe('passage-p1')
      expect(response.references[1].draftVersion).toBe(2)
    } finally {
      global.fetch = originalFetch
    }
  })

  test('sends stable Tutor context identifiers without client answer or score data', async () => {
    const { sendTutorMessage } = await import('../services/aiTutorApi')
    const originalFetch = global.fetch
    global.fetch = async (_url, options) => {
      const body = JSON.parse(options.body)
      expect(body.context).toEqual(expect.objectContaining({
        skill: 'READING',
        exerciseId: 'reading-foundation-01',
        questionId: 'reading-q1',
        attemptId: 'attempt-1',
      }))
      expect(body.context).not.toHaveProperty('correctAnswer')
      expect(body.context).not.toHaveProperty('score')
      return { ok: true, status: 200, json: async () => ({ status: 'APP_DATA', answer: 'Bạn đã chọn C.' }) }
    }

    try {
      await sendTutorMessage({
        message: 'đáp án tôi vừa chọn',
        context: { skill: 'READING', exerciseId: 'reading-foundation-01', questionId: 'reading-q1', attemptId: 'attempt-1' },
      })
    } finally {
      global.fetch = originalFetch
    }
  })
})

