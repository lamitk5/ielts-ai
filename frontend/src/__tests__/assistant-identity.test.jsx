import { act, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, test, vi } from 'vitest'
import FloatingTutor from '../components/tutor/FloatingTutor'
import { PreferenceProvider } from '../features/preferences/PreferenceProvider'
import { writeGuestPreferences } from '../features/preferences/preferenceStorage'

function renderTutor() {
  return render(
    <MemoryRouter>
      <PreferenceProvider>
        <FloatingTutor />
      </PreferenceProvider>
    </MemoryRouter>,
  )
}

afterEach(() => {
  vi.useRealTimers()
  vi.restoreAllMocks()
  localStorage.clear()
})

describe('Én assistant identity', () => {
  test('uses Én across the launcher and proactive reminder cadence', () => {
    vi.useFakeTimers()
    vi.spyOn(Math, 'random').mockReturnValueOnce(0).mockReturnValueOnce(0.1)
    writeGuestPreferences({ proactiveAiEnabled: true })

    renderTutor()

    expect(screen.getByRole('button', { name: 'Mở Én' })).toBeInTheDocument()
    expect(screen.getByText('Én')).toHaveClass('ai-tutor-mascot-tooltip')

    act(() => vi.advanceTimersByTime(17_999))
    expect(screen.queryByRole('button', { name: /Mở Én:/ })).not.toBeInTheDocument()

    act(() => vi.advanceTimersByTime(1))
    const reminder = screen.getByRole('button', { name: /^Mở Én: / })
    expect(reminder).toHaveAccessibleName(/Én/)
  })
})
