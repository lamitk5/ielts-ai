import { act, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { AuthProvider } from '../features/auth/AuthProvider'
import { useOptionalAuth } from '../features/auth/AuthProvider'
import { PreferenceProvider, usePreferences } from '../features/preferences/PreferenceProvider'
import { DEFAULT_PREFERENCES } from '../features/preferences/preferenceDefaults'
import { PREFERENCE_STORAGE_KEY, readAccountPreferenceCache, readGuestPreferences, writeGuestPreferences } from '../features/preferences/preferenceStorage'
import { getPreferences, savePreferences } from '../services/preferencesApi'

const account = { id: 'member-1', email: 'member@example.com' }
const server = (accentPreset = 'SAPPHIRE', version = 4, language = 'VI') => ({
  themeMode: 'SYSTEM', accentPreset, fontScale: 'DEFAULT', density: 'DEFAULT',
  reduceMotion: 'SYSTEM', proactiveAiEnabled: false, crossHighlightEnabled: true,
  timerDefaultEnabled: false, readingSplitRatio: 40, writingSplitRatio: 40, language, version,
})

function Probe() {
  const { preferences, updatePreference, status, confirmedPreferences, retry } = usePreferences()
  const auth = useOptionalAuth()
  return <>
    <button onClick={() => updatePreference('accentPreset', 'emerald')}>Emerald</button>
    <button onClick={() => updatePreference('accentPreset', 'violet')}>Violet</button>
    <button onClick={retry}>Retry</button>
    {auth ? <button onClick={() => auth.logout()}>Logout</button> : null}
    {auth ? <button onClick={() => auth.login({ email: 'other@example.com', password: 'secret' })}>Switch</button> : null}
    <span data-testid="accent">{preferences.accentPreset}</span>
    <span data-testid="status">{status}</span>
    <span data-testid="confirmed">{confirmedPreferences?.accentPreset ?? 'none'}</span>
  </>
}

beforeEach(() => localStorage.clear())
afterEach(() => { vi.useRealTimers(); vi.unstubAllGlobals(); vi.restoreAllMocks() })

describe('preference persistence', () => {
  test('guest record is versioned and contains only normalized preferences', () => {
    expect(writeGuestPreferences({ accentPreset: 'emerald', secret: 'private' })).toBe(true)
    expect(JSON.parse(localStorage.getItem(PREFERENCE_STORAGE_KEY))).toEqual({ version: 1, preferences: { ...DEFAULT_PREFERENCES, accentPreset: 'emerald' } })
    expect(readGuestPreferences()).toEqual({ ...DEFAULT_PREFERENCES, accentPreset: 'emerald' })
  })

  test('malformed and old guest records fall back safely', () => {
    for (const record of ['{bad', JSON.stringify({ version: 0, preferences: { accentPreset: 'violet' } })]) {
      localStorage.setItem(PREFERENCE_STORAGE_KEY, record)
      expect(readGuestPreferences()).toEqual(DEFAULT_PREFERENCES)
      expect(localStorage.getItem(PREFERENCE_STORAGE_KEY)).toBeNull()
    }
  })

  test('invalid stored enum, boolean, ratio, and account cache are discarded', () => {
    for (const preferences of [
      { ...DEFAULT_PREFERENCES, accentPreset: 'unknown' },
      { ...DEFAULT_PREFERENCES, proactiveAiEnabled: 'yes' },
      { ...DEFAULT_PREFERENCES, readingSplitRatio: 45 },
    ]) {
      localStorage.setItem(PREFERENCE_STORAGE_KEY, JSON.stringify({ version: 1, preferences }))
      expect(readGuestPreferences()).toEqual(DEFAULT_PREFERENCES)
      expect(localStorage.getItem(PREFERENCE_STORAGE_KEY)).toBeNull()
    }
    const key = 'ielts-ai-tutor.preferences.account.member-1.v1'
    localStorage.setItem(key, JSON.stringify({ version: 1, preferences: { ...DEFAULT_PREFERENCES, density: 'invalid' }, serverVersion: 4 }))
    expect(readAccountPreferenceCache('member-1')).toBeNull()
    expect(localStorage.getItem(key)).toBeNull()
  })

  test('wire contract maps complete uppercase Java enums in both directions', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
    const fetchMock = vi.fn().mockResolvedValueOnce({ ok: true, json: async () => server('EMERALD', 4, 'EN') })
      .mockResolvedValueOnce({ ok: true, json: async () => server('VIOLET', 5, 'EN') })
    vi.stubGlobal('fetch', fetchMock)
    expect(await getPreferences()).toEqual({ ...DEFAULT_PREFERENCES, accentPreset: 'emerald', language: 'en', version: 4 })
    expect(await savePreferences({ ...DEFAULT_PREFERENCES, themeMode: 'dark', density: 'spacious', reduceMotion: 'reduce', accentPreset: 'violet' }, 4))
      .toEqual({ ...DEFAULT_PREFERENCES, accentPreset: 'violet', language: 'en', version: 5 })
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({
      ...server('VIOLET', 4), themeMode: 'DARK', density: 'COMFORTABLE', reduceMotion: 'REDUCED',
    })
  })

  test('malformed successful server record is rejected without replacing cached preview', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
    localStorage.setItem('ielts-ai-tutor.preferences.account.member-1.v1', JSON.stringify({ version: 1, preferences: { ...DEFAULT_PREFERENCES, accentPreset: 'emerald' }, serverVersion: 3 }))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ version: 5 }) }))
    render(<AuthProvider><PreferenceProvider><Probe /></PreferenceProvider></AuthProvider>)
    await screen.findByText('unsynced')
    expect(screen.getByTestId('accent')).toHaveTextContent('emerald')
    expect(readAccountPreferenceCache('member-1').preferences.accentPreset).toBe('emerald')
  })

  test('storage errors preserve the immediate guest preview and expose unsynced state', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('quota') })
    render(<PreferenceProvider><Probe /></PreferenceProvider>)
    act(() => screen.getByRole('button', { name: 'Emerald' }).click())
    expect(screen.getByTestId('accent')).toHaveTextContent('emerald')
    expect(screen.getByTestId('status')).toHaveTextContent('unsynced')
  })

  test('server wins login hydration without copying guest preferences', async () => {
    localStorage.setItem(PREFERENCE_STORAGE_KEY, JSON.stringify({ version: 1, preferences: { ...DEFAULT_PREFERENCES, accentPreset: 'violet' } }))
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: async () => server() })
    vi.stubGlobal('fetch', fetchMock)
    render(<AuthProvider><PreferenceProvider><Probe /></PreferenceProvider></AuthProvider>)
    expect(screen.getByTestId('accent')).not.toHaveTextContent('violet')
    await waitFor(() => expect(screen.getByTestId('accent')).toHaveTextContent('sapphire'))
    expect(screen.getByTestId('status')).toHaveTextContent('synced')
    expect(fetchMock).toHaveBeenCalledWith('/api/user/preferences', expect.objectContaining({ headers: expect.objectContaining({ Authorization: 'Bearer token' }) }))
    expect(JSON.parse(localStorage.getItem(PREFERENCE_STORAGE_KEY)).preferences.accentPreset).toBe('violet')
  })

  test('edit made during delayed GET survives hydration and autosaves against server version', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
    let resolveGet
    const pendingGet = new Promise((resolve) => { resolveGet = resolve })
    const fetchMock = vi.fn().mockReturnValueOnce(pendingGet).mockResolvedValueOnce({ ok: true, json: async () => server('EMERALD', 5) })
    vi.stubGlobal('fetch', fetchMock)
    render(<AuthProvider><PreferenceProvider><Probe /></PreferenceProvider></AuthProvider>)
    vi.useFakeTimers()
    act(() => screen.getByRole('button', { name: 'Emerald' }).click())
    await act(async () => { await vi.advanceTimersByTimeAsync(350) })
    expect(screen.getByTestId('accent')).toHaveTextContent('emerald')
    await act(async () => { resolveGet({ ok: true, json: async () => server() }); await Promise.resolve() })
    expect(screen.getByTestId('accent')).toHaveTextContent('emerald')
    await act(async () => { await vi.advanceTimersByTimeAsync(350) })
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual(server('EMERALD', 4))
    expect(screen.getByTestId('status')).toHaveTextContent('synced')
  })

  test('edit during failed initial GET remains visible and saves after automatic hydration retry', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
    let rejectGet
    const pendingGet = new Promise((_resolve, reject) => { rejectGet = reject })
    const fetchMock = vi.fn().mockReturnValueOnce(pendingGet)
      .mockResolvedValueOnce({ ok: true, json: async () => server() })
      .mockResolvedValueOnce({ ok: true, json: async () => server('EMERALD', 5) })
    vi.stubGlobal('fetch', fetchMock)
    render(<AuthProvider><PreferenceProvider><Probe /></PreferenceProvider></AuthProvider>)
    vi.useFakeTimers()
    act(() => screen.getByRole('button', { name: 'Emerald' }).click())
    await act(async () => { rejectGet(new Error('offline')); await Promise.resolve() })
    expect(screen.getByTestId('accent')).toHaveTextContent('emerald')
    await act(async () => { await vi.advanceTimersByTimeAsync(700) })
    expect(JSON.parse(fetchMock.mock.calls[2][1].body).version).toBe(4)
    expect(screen.getByTestId('status')).toHaveTextContent('synced')
  })

  test('conflict hydration preserves a newer edit and saves it against the reconciled version', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
    let resolveConflictGet
    const pendingConflictGet = new Promise((resolve) => { resolveConflictGet = resolve })
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => server() })
      .mockResolvedValueOnce({ ok: false, status: 409, json: async () => ({ error: { code: 'VERSION_CONFLICT' } }) })
      .mockReturnValueOnce(pendingConflictGet)
      .mockResolvedValueOnce({ ok: true, json: async () => server('VIOLET', 6) })
    vi.stubGlobal('fetch', fetchMock)
    render(<AuthProvider><PreferenceProvider><Probe /></PreferenceProvider></AuthProvider>)
    await screen.findByText('synced')
    vi.useFakeTimers()
    act(() => screen.getByRole('button', { name: 'Emerald' }).click())
    await act(async () => { await vi.advanceTimersByTimeAsync(300) })
    expect(fetchMock).toHaveBeenCalledTimes(3)
    act(() => screen.getByRole('button', { name: 'Violet' }).click())
    await act(async () => { resolveConflictGet({ ok: true, json: async () => server('BURGUNDY', 5) }); await Promise.resolve() })
    expect(screen.getByTestId('accent')).toHaveTextContent('violet')
    await act(async () => { await vi.advanceTimersByTimeAsync(350) })
    expect(JSON.parse(fetchMock.mock.calls[3][1].body)).toEqual(server('VIOLET', 5))
    expect(screen.getByTestId('accent')).toHaveTextContent('violet')
  })

  test('persistent hydration failure stops automatic retries and explicit retry can recover', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(new Error('offline'))
      .mockRejectedValueOnce(new Error('still offline'))
      .mockResolvedValueOnce({ ok: true, json: async () => server() })
      .mockResolvedValueOnce({ ok: true, json: async () => server('EMERALD', 5) })
    vi.stubGlobal('fetch', fetchMock)
    render(<AuthProvider><PreferenceProvider><Probe /></PreferenceProvider></AuthProvider>)
    vi.useFakeTimers()
    act(() => screen.getByRole('button', { name: 'Emerald' }).click())
    await act(async () => { await vi.advanceTimersByTimeAsync(1000) })
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(screen.getByTestId('accent')).toHaveTextContent('emerald')
    expect(screen.getByTestId('status')).toHaveTextContent('unsynced')
    act(() => screen.getByRole('button', { name: 'Retry' }).click())
    await act(async () => { await vi.advanceTimersByTimeAsync(350) })
    expect(fetchMock).toHaveBeenCalledTimes(4)
    expect(screen.getByTestId('accent')).toHaveTextContent('emerald')
    expect(screen.getByTestId('status')).toHaveTextContent('synced')
  })

  test.each(['logout', 'switch'])('late retry GET cannot restore old account after %s', async (action) => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
    const oldKey = 'ielts-ai-tutor.preferences.account.member-1.v1'
    localStorage.setItem(oldKey, JSON.stringify({ version: 1, preferences: DEFAULT_PREFERENCES, serverVersion: 3 }))
    let resolveRetry
    const pendingRetry = new Promise((resolve) => { resolveRetry = resolve })
    let getCount = 0
    vi.stubGlobal('fetch', vi.fn((url, options) => {
      if (url === '/api/user/preferences' && options.method === 'GET') {
        getCount += 1
        if (getCount === 1) return Promise.reject(new Error('offline'))
        if (getCount === 2) return pendingRetry
        return Promise.resolve({ ok: true, json: async () => server('GOLD') })
      }
      if (url === '/api/auth/login') return Promise.resolve({ ok: true, json: async () => ({ token: 'new-token', user: { id: 'member-2' } }) })
      return Promise.resolve({ ok: true, status: 204, json: async () => null })
    }))
    render(<AuthProvider><PreferenceProvider><Probe /></PreferenceProvider></AuthProvider>)
    await screen.findByText('unsynced')
    act(() => screen.getByRole('button', { name: 'Retry' }).click())
    screen.getByRole('button', { name: action === 'logout' ? 'Logout' : 'Switch' }).click()
    await waitFor(() => expect(localStorage.getItem('ielts-ai-tutor.session')?.includes('member-1') ?? false).toBe(false))
    await act(async () => { resolveRetry({ ok: true, json: async () => server('VIOLET', 5) }); await Promise.resolve() })
    expect(localStorage.getItem(oldKey)).toBeNull()
    if (action === 'switch') expect(screen.getByTestId('accent')).toHaveTextContent('gold')
  })

  test('authenticated edit previews immediately, debounces, and accepts authoritative save response', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
    const fetchMock = vi.fn().mockResolvedValueOnce({ ok: true, json: async () => server() }).mockResolvedValueOnce({ ok: true, json: async () => server('GOLD', 5) })
    vi.stubGlobal('fetch', fetchMock)
    render(<AuthProvider><PreferenceProvider><Probe /></PreferenceProvider></AuthProvider>)
    await screen.findByText('synced')
    vi.useFakeTimers()
    act(() => { screen.getByRole('button', { name: 'Emerald' }).click(); screen.getByRole('button', { name: 'Violet' }).click() })
    expect(screen.getByTestId('accent')).toHaveTextContent('violet')
    expect(fetchMock).toHaveBeenCalledTimes(1)
    await act(async () => { await vi.advanceTimersByTimeAsync(350) })
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual(server('VIOLET', 4))
    expect(screen.getByTestId('accent')).toHaveTextContent('gold')
    expect(screen.getByTestId('status')).toHaveTextContent('synced')
  })

  test('conflict refetches server record and reports conflict', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
    const fetchMock = vi.fn().mockResolvedValueOnce({ ok: true, json: async () => server() }).mockResolvedValueOnce({ ok: false, status: 409, json: async () => ({ error: { code: 'VERSION_CONFLICT' } }) }).mockResolvedValueOnce({ ok: true, json: async () => server('BURGUNDY', 6) })
    vi.stubGlobal('fetch', fetchMock)
    render(<AuthProvider><PreferenceProvider><Probe /></PreferenceProvider></AuthProvider>)
    await screen.findByText('synced')
    vi.useFakeTimers()
    act(() => screen.getByRole('button', { name: 'Emerald' }).click())
    await act(async () => { await vi.advanceTimersByTimeAsync(350) })
    expect(screen.getByTestId('accent')).toHaveTextContent('burgundy')
    expect(screen.getByTestId('status')).toHaveTextContent('conflict')
  })

  test('failed save retains preview and confirmed server value with unsynced status', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce({ ok: true, json: async () => server() }).mockRejectedValueOnce(new Error('offline')))
    render(<AuthProvider><PreferenceProvider><Probe /></PreferenceProvider></AuthProvider>)
    await screen.findByText('synced')
    vi.useFakeTimers()
    act(() => screen.getByRole('button', { name: 'Emerald' }).click())
    await act(async () => { await vi.advanceTimersByTimeAsync(350) })
    expect(screen.getByTestId('accent')).toHaveTextContent('emerald')
    expect(screen.getByTestId('confirmed')).toHaveTextContent('sapphire')
    expect(screen.getByTestId('status')).toHaveTextContent('unsynced')
  })

  test('a later edit waits for an in-flight save and uses its new version', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
    let finishFirstSave
    const firstSave = new Promise((resolve) => { finishFirstSave = resolve })
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => server() })
      .mockReturnValueOnce(firstSave)
      .mockResolvedValueOnce({ ok: true, json: async () => server('VIOLET', 6) })
    vi.stubGlobal('fetch', fetchMock)
    render(<AuthProvider><PreferenceProvider><Probe /></PreferenceProvider></AuthProvider>)
    await screen.findByText('synced')
    vi.useFakeTimers()
    act(() => screen.getByRole('button', { name: 'Emerald' }).click())
    await act(async () => { await vi.advanceTimersByTimeAsync(300) })
    act(() => screen.getByRole('button', { name: 'Violet' }).click())
    await act(async () => { await vi.advanceTimersByTimeAsync(300) })
    expect(fetchMock).toHaveBeenCalledTimes(2)
    await act(async () => { finishFirstSave({ ok: true, json: async () => server('EMERALD', 5) }); await Promise.resolve() })
    await act(async () => { await vi.advanceTimersByTimeAsync(300) })
    expect(JSON.parse(fetchMock.mock.calls[2][1].body).version).toBe(5)
    expect(screen.getByTestId('accent')).toHaveTextContent('violet')
  })

  test('newest edit is requeued after an older in-flight save fails', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
    let rejectFirstSave
    const pendingSave = new Promise((_resolve, reject) => { rejectFirstSave = reject })
    const fetchMock = vi.fn().mockResolvedValueOnce({ ok: true, json: async () => server() })
      .mockReturnValueOnce(pendingSave)
      .mockResolvedValueOnce({ ok: true, json: async () => server('VIOLET', 5) })
    vi.stubGlobal('fetch', fetchMock)
    render(<AuthProvider><PreferenceProvider><Probe /></PreferenceProvider></AuthProvider>)
    await screen.findByText('synced')
    vi.useFakeTimers()
    act(() => screen.getByRole('button', { name: 'Emerald' }).click())
    await act(async () => { await vi.advanceTimersByTimeAsync(300) })
    act(() => screen.getByRole('button', { name: 'Violet' }).click())
    await act(async () => { await vi.advanceTimersByTimeAsync(300) })
    await act(async () => { rejectFirstSave(new Error('offline')); await Promise.resolve() })
    await act(async () => { await vi.advanceTimersByTimeAsync(350) })
    expect(JSON.parse(fetchMock.mock.calls[2][1].body)).toEqual(server('VIOLET', 4))
    expect(screen.getByTestId('accent')).toHaveTextContent('violet')
    expect(screen.getByTestId('status')).toHaveTextContent('synced')
  })
})
