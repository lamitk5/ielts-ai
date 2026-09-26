import { describe, expect, test } from 'vitest'
import { normalizeTutorReferences } from '../features/tutor/tutorReferenceSchema'

describe('Tutor account lifecycle boundary', () => {
  test('keeps conversation references normalized and provider-neutral', () => {
    const references = normalizeTutorReferences([
      { referenceType: 'PASSAGE', targetId: 'source-1', label: 'Reading source' },
      { provider: 'gemini', rawPayload: 'must not cross the UI boundary' },
    ])

    expect(references).toEqual([expect.objectContaining({ referenceType: 'PASSAGE', targetId: 'source-1' })])
    expect(JSON.stringify(references)).not.toMatch(/gemini|rawPayload/i)
  })
})
