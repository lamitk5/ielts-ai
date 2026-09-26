import { describe, expect, test, vi, beforeEach } from 'vitest'
import {
  createPracticeGeneratorApi,
  PracticeGeneratorApiError,
} from '../features/practice-generator/practiceGeneratorApi'

describe('practiceGeneratorApi', () => {
  let fetchMock

  beforeEach(() => {
    window.localStorage.clear()
    fetchMock = vi.fn()
  })

  test('attaches bearer token from authenticated session', async () => {
    window.localStorage.setItem(
      'ielts-ai-tutor.session',
      JSON.stringify({ token: 'admin-jwt-token', user: { role: 'ADMIN' } })
    )

    fetchMock.mockResolvedValueOnce({
      ok: true,
      json: async () => [{ id: 'src-1', title: 'Source 1' }],
    })

    const api = createPracticeGeneratorApi({ fetchImpl: fetchMock })
    const sources = await api.listSources('APPROVED')

    expect(sources).toHaveLength(1)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/admin/practice-generator/sources?rightsStatus=APPROVED',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer admin-jwt-token',
          'Content-Type': 'application/json',
        }),
      })
    )
  })

  test('registerSource sends POST with source data', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ id: 'src-123', title: 'New Source', rightsStatus: 'APPROVED' }),
    })

    const api = createPracticeGeneratorApi({ fetchImpl: fetchMock })
    const payload = {
      title: 'New Source',
      rawText: 'Academic reading passage content...',
      rightsStatus: 'APPROVED',
      language: 'en',
      skill: 'reading',
    }

    const result = await api.registerSource(payload)
    expect(result.id).toBe('src-123')
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/admin/practice-generator/sources',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(payload),
      })
    )
  })

  test('createJob sends job creation payload', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ jobId: 'job-1', status: 'GENERATING' }),
    })

    const api = createPracticeGeneratorApi({ fetchImpl: fetchMock })
    const jobPayload = { sourceId: 'src-1', blueprintId: 'bp-1', skill: 'READING' }
    const result = await api.createJob(jobPayload)

    expect(result.jobId).toBe('job-1')
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/admin/practice-generator/jobs',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(jobPayload),
      })
    )
  })

  test('submitReview posts review action payload', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ action: 'APPROVE', resultingState: 'APPROVED' }),
    })

    const api = createPracticeGeneratorApi({ fetchImpl: fetchMock })
    const reviewData = { action: 'APPROVE', feedbackNotes: 'Curated' }
    const result = await api.submitReview('set-99', reviewData)

    expect(result.action).toBe('APPROVE')
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/admin/practice-generator/sets/set-99/review',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(reviewData),
      })
    )
  })

  test('handles API errors throwing PracticeGeneratorApiError', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: false,
      status: 403,
      json: async () => ({
        error: { code: 'AUTH_FORBIDDEN', message: 'Admin role required' },
      }),
    })

    const api = createPracticeGeneratorApi({ fetchImpl: fetchMock })
    await expect(api.listJobs()).rejects.toThrow(PracticeGeneratorApiError)
  })
})
