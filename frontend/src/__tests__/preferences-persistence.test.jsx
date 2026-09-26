import { act, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { AuthProvider } from '../features/auth/AuthProvider'
import { PreferenceProvider, usePreferences } from '../features/preferences/PreferenceProvider'
import { DEFAULT_PREFERENCES } from '../features/preferences/preferenceDefaults'
import { PREFERENCE_STORAGE_KEY, readGuestPreferences, writeGuestPreferences } from '../features/preferences/preferenceStorage'

const account = { id: 'member-1', email: 'member@example.com' }
const server = (accentPreset = 'sapphire', version = 4) => ({ ...DEFAULT_PREFERENCES, accentPreset, version })

function Probe() {
  const { preferences, updatePreference, status, confirmedPreferences } = usePreferences()
  return <>
    <button onClick={() => updatePreference('accentPreset', 'emerald')}>Emerald</button>
    <button onClick={() => updatePreference('accentPreset', 'violet')}>Violet</button>
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

  test('authenticated edit previews immediately, debounces, and accepts authoritative save response', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
    const fetchMock = vi.fn().mockResolvedValueOnce({ ok: true, json: async () => server() }).mockResolvedValueOnce({ ok: true, json: async () => server('gold', 5) })
    vi.stubGlobal('fetch', fetchMock)
    render(<AuthProvider><PreferenceProvider><Probe /></PreferenceProvider></AuthProvider>)
    await screen.findByText('synced')
    vi.useFakeTimers()
    act(() => { screen.getByRole('button', { name: 'Emerald' }).click(); screen.getByRole('button', { name: 'Violet' }).click() })
    expect(screen.getByTestId('accent')).toHaveTextContent('violet')
    expect(fetchMock).toHaveBeenCalledTimes(1)
    await act(async () => { await vi.advanceTimersByTimeAsync(350) })
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({ ...DEFAULT_PREFERENCES, accentPreset: 'violet', version: 4 })
    expect(screen.getByTestId('accent')).toHaveTextContent('gold')
    expect(screen.getByTestId('status')).toHaveTextContent('synced')
  })

  test('conflict refetches server record and reports conflict', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
    const fetchMock = vi.fn().mockResolvedValueOnce({ ok: true, json: async () => server() }).mockResolvedValueOnce({ ok: false, status: 409, json: async () => ({ error: { code: 'VERSION_CONFLICT' } }) }).mockResolvedValueOnce({ ok: true, json: async () => server('burgundy', 6) })
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
      .mockResolvedValueOnce({ ok: true, json: async () => server('violet', 6) })
    vi.stubGlobal('fetch', fetchMock)
    render(<AuthProvider><PreferenceProvider><Probe /></PreferenceProvider></AuthProvider>)
    await screen.findByText('synced')
    vi.useFakeTimers()
    act(() => screen.getByRole('button', { name: 'Emerald' }).click())
    await act(async () => { await vi.advanceTimersByTimeAsync(300) })
    act(() => screen.getByRole('button', { name: 'Violet' }).click())
    await act(async () => { await vi.advanceTimersByTimeAsync(300) })
    expect(fetchMock).toHaveBeenCalledTimes(2)
    await act(async () => { finishFirstSave({ ok: true, json: async () => server('emerald', 5) }); await Promise.resolve() })
    await act(async () => { await vi.advanceTimersByTimeAsync(300) })
    expect(JSON.parse(fetchMock.mock.calls[2][1].body).version).toBe(5)
    expect(screen.getByTestId('accent')).toHaveTextContent('violet')
  })
})
