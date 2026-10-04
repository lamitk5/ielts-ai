import { act, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { AuthProvider } from '../features/auth/AuthProvider'
import { OnboardingPanel } from '../components/onboarding/OnboardingPanel'
import { OnboardingEntry } from '../components/onboarding/OnboardingEntry'
import { getOnboarding, saveOnboarding } from '../services/onboardingApi'

const account = { id: 'member-1', email: 'member@example.com' }

const emptyProfile = {
  userId: 'member-1', selfReportedLevel: null, targetBand: null, targetExamDate: null,
  perceivedWeakestSkill: null, dailyStudyMinutes: null, studyDaysPerWeek: null,
  state: 'NOT_STARTED', source: 'SELF_REPORTED', basis: 'LEARNER_DECLARATION',
  measuredLevel: null, measuredWeakestSkill: null, evidenceReference: null,
  version: 0, createdAt: '2026-10-01T00:00:00Z', updatedAt: '2026-10-01T00:00:00Z',
}

function jsonResponse(body, status = 200) {
  return { ok: status < 400, status, json: async () => body }
}

beforeEach(() => localStorage.clear())
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks() })

describe('onboarding api', () => {
  test('requires a session and normalizes the self-reported profile', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 't-1' }))
    const fetchMock = vi.fn(async () => jsonResponse({ ...emptyProfile, targetBand: 6.5, version: 2 }))
    vi.stubGlobal('fetch', fetchMock)

    const profile = await getOnboarding()

    expect(fetchMock).toHaveBeenCalledWith('/api/me/onboarding',
      expect.objectContaining({ method: 'GET' }))
    expect(profile.targetBand).toBe(6.5)
    expect(profile.source).toBe('SELF_REPORTED')
    expect(profile.isSelfReportOnly).toBe(true)
  })

  test('refuses to call the API without a token', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    await expect(getOnboarding()).rejects.toThrow('AUTH_REQUIRED')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  test('never forwards client measured evidence or owner identity', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 't-1' }))
    const fetchMock = vi.fn(async () => jsonResponse({ ...emptyProfile, version: 1 }))
    vi.stubGlobal('fetch', fetchMock)

    await saveOnboarding({
      selfReportedLevel: 'BEGINNER', targetBand: 5.0, dailyStudyMinutes: 30,
      studyDaysPerWeek: 3, state: 'COMPLETED', version: 0,
      userId: 'someone-else', measuredLevel: 'ADVANCED', evidenceReference: 'fabricated',
    })

    const body = JSON.parse(fetchMock.mock.calls[0][1].body)
    expect(body.userId).toBeUndefined()
    expect(body.measuredLevel).toBeUndefined()
    expect(body.evidenceReference).toBeUndefined()
    expect(body.selfReportedLevel).toBe('BEGINNER')
  })

  test('surfaces a version conflict instead of overwriting silently', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 't-1' }))
    vi.stubGlobal('fetch', vi.fn(async () => jsonResponse({}, 409)))

    await expect(saveOnboarding({ version: 0, state: 'COMPLETED' })).rejects.toMatchObject({ code: 'CONFLICT' })
  })
})

describe('OnboardingPanel', () => {
  beforeEach(() => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 't-1', user: account }))
  })

  test('offers skip without inventing goals and labels self-report as a belief', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => jsonResponse(emptyProfile)))

    render(<AuthProvider><OnboardingPanel /></AuthProvider>)

    expect(await screen.findByText(/mục tiêu của bạn/i)).toBeTruthy()
    expect(screen.getByLabelText(/bạn tự nhận định/i)).toBeTruthy()
    expect(screen.queryByText(/trình độ hiện tại của bạn là/i)).toBeNull()
  })

  test('skip records the choice and shows an honest neutral state', async () => {
    const calls = []
    vi.stubGlobal('fetch', vi.fn(async (url, init) => {
      calls.push({ url, method: init?.method })
      if (url.endsWith('/complete')) return jsonResponse({ ...emptyProfile, state: 'SKIPPED', version: 1 })
      return jsonResponse(emptyProfile)
    }))

    render(<AuthProvider><OnboardingPanel /></AuthProvider>)
    const skip = await screen.findByRole('button', { name: /bỏ qua/i })
    await act(async () => { skip.click() })

    await waitFor(() => expect(calls.some((call) => call.method === 'POST')).toBe(true))
    expect(await screen.findByText(/bạn có thể thiết lập sau/i)).toBeTruthy()
  })

  test('does not present a measured level when the server only has self-report', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => jsonResponse({
      ...emptyProfile, selfReportedLevel: 'BEGINNER', state: 'COMPLETED', version: 1,
    })))

    render(<AuthProvider><OnboardingPanel /></AuthProvider>)

    expect(await screen.findByText(/ước tính/i)).toBeTruthy()
    expect(screen.queryByText(/band chính thức/i)).toBeNull()
  })
})

describe('OnboardingEntry', () => {
  test('a guest is never asked to set goals', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    render(<AuthProvider><OnboardingEntry /></AuthProvider>)

    await waitFor(() => expect(fetchMock).not.toHaveBeenCalled())
    expect(screen.queryByText(/thiết lập mục tiêu học tập/i)).toBeNull()
  })

  test('a learner who already finished setup sees no setup prompt', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 't-1', user: account }))
    vi.stubGlobal('fetch', vi.fn(async () => jsonResponse({ ...emptyProfile, state: 'COMPLETED', version: 1 })))

    render(<AuthProvider><OnboardingEntry /></AuthProvider>)

    await waitFor(() => expect(screen.queryByText(/thiết lập mục tiêu học tập/i)).toBeNull())
  })

  test('a learner without goals gets an optional entry that opens the form', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 't-1', user: account }))
    vi.stubGlobal('fetch', vi.fn(async () => jsonResponse(emptyProfile)))

    render(<AuthProvider><OnboardingEntry /></AuthProvider>)
    const open = await screen.findByRole('button', { name: /thiết lập mục tiêu/i })
    await act(async () => { open.click() })

    expect(await screen.findByRole('button', { name: /bỏ qua/i })).toBeTruthy()
  })
})