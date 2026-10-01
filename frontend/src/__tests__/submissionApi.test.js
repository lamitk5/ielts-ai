import { describe, expect, it, vi } from 'vitest'

import {
  startCanonicalSubmission,
  autosaveCanonicalSubmission,
  submitCanonicalSubmission,
  getCanonicalSubmission,
} from '../services/submissionsApi'

describe('canonical submission API client', () => {
  it('uses server-authoritative submission endpoints without score fields', async () => {
    const response = { id: 'submission-1', status: 'IN_PROGRESS' }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => response }))

    await startCanonicalSubmission({ publishedSetId: 'set-1', skill: 'reading', idempotencyKey: 'start-1' })
    await getCanonicalSubmission('submission-1')
    await autosaveCanonicalSubmission('submission-1', { q1: 'B' }, 0, 'draft-1')
    await submitCanonicalSubmission('submission-1', { q1: 'B' }, 'submit-1')

    const calls = fetch.mock.calls
    expect(calls[0][0]).toBe('/api/submissions')
    expect(JSON.parse(calls[0][1].body)).toEqual({ publishedSetId: 'set-1', skill: 'reading', idempotencyKey: 'start-1' })
    expect(JSON.parse(calls[3][1].body)).toEqual({ payload: { q1: 'B' }, idempotencyKey: 'submit-1' })
    expect(JSON.stringify(calls)).not.toContain('score')
  })
})
