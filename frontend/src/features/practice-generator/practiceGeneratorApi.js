const SESSION_KEY = 'ielts-ai-tutor.session'

function readSession() {
  try {
    return JSON.parse(window.localStorage.getItem(SESSION_KEY) ?? 'null')
  } catch {
    return null
  }
}

export class PracticeGeneratorApiError extends Error {
  constructor(code, message, status) {
    super(message)
    this.name = 'PracticeGeneratorApiError'
    this.code = code
    this.status = status
  }
}

export function createPracticeGeneratorApi({ fetchImpl = window.fetch.bind(window) } = {}) {
  async function request(path, options = {}) {
    if (!path.startsWith('/api/admin/practice-generator')) {
      throw new Error('Practice generator admin requests must be same-origin')
    }
    const session = readSession()
    const headers = {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
    }

    const response = await fetchImpl(path, {
      ...options,
      headers,
    })

    const payload = await response.json().catch(() => null)
    if (!response.ok) {
      throw new PracticeGeneratorApiError(
        payload?.error?.code ?? `HTTP_${response.status}`,
        payload?.error?.message ?? 'Không thể kết nối đến hệ thống tạo đề.',
        response.status
      )
    }
    return payload
  }

  return {
    listSources: (rightsStatus) =>
      request(`/api/admin/practice-generator/sources${rightsStatus ? `?rightsStatus=${encodeURIComponent(rightsStatus)}` : ''}`),

    registerSource: (sourceData) =>
      request('/api/admin/practice-generator/sources', {
        method: 'POST',
        body: JSON.stringify(sourceData),
      }),

    listBlueprints: (skill) =>
      request(`/api/admin/practice-generator/blueprints${skill ? `?skill=${encodeURIComponent(skill)}` : ''}`),

    extractBlueprint: (payload) =>
      request('/api/admin/practice-generator/blueprints/extract', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),

    listJobs: (limit = 50, offset = 0) =>
      request(`/api/admin/practice-generator/jobs?limit=${limit}&offset=${offset}`),

    createJob: (jobPayload) =>
      request('/api/admin/practice-generator/jobs', {
        method: 'POST',
        body: JSON.stringify(jobPayload),
      }),

    getJobStatus: (jobId) =>
      request(`/api/admin/practice-generator/jobs/${encodeURIComponent(jobId)}`),

    listSets: (state) =>
      request(`/api/admin/practice-generator/sets${state ? `?state=${encodeURIComponent(state)}` : ''}`),

    getSetReviewPayload: (setId) =>
      request(`/api/admin/practice-generator/sets/${encodeURIComponent(setId)}`),

    submitReview: (setId, reviewData) =>
      request(`/api/admin/practice-generator/sets/${encodeURIComponent(setId)}/review`, {
        method: 'POST',
        body: JSON.stringify(reviewData),
      }),

    regenerateItem: (setId, itemData) =>
      request(`/api/admin/practice-generator/sets/${encodeURIComponent(setId)}/items/regenerate`, {
        method: 'POST',
        body: JSON.stringify(itemData),
      }),

    applyManualEdit: (setId, editData) =>
      request(`/api/admin/practice-generator/sets/${encodeURIComponent(setId)}/edit`, {
        method: 'POST',
        body: JSON.stringify(editData),
      }),

    compareVersions: (setId, v1, v2) =>
      request(`/api/admin/practice-generator/sets/${encodeURIComponent(setId)}/compare?v1=${v1}&v2=${v2}`),

    getVersionHistory: (setId) =>
      request(`/api/admin/practice-generator/sets/${encodeURIComponent(setId)}/versions`),

    getAuditHistory: (setId) =>
      request(`/api/admin/practice-generator/sets/${encodeURIComponent(setId)}/audit`),
  }
}

export const practiceGeneratorApi = createPracticeGeneratorApi()
