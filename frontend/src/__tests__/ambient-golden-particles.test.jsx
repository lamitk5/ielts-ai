import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, test, vi } from 'vitest'
import App from '../App'
import AmbientGoldenParticles from '../components/common/AmbientGoldenParticles'
import { PreferenceProvider } from '../features/preferences/PreferenceProvider'
import { writeGuestPreferences } from '../features/preferences/preferenceStorage'

afterEach(() => {
  localStorage.clear()
  document.documentElement.removeAttribute('data-reduced-motion')
  vi.restoreAllMocks()
})

function renderParticles() {
  return render(
    <PreferenceProvider>
      <AmbientGoldenParticles />
    </PreferenceProvider>,
  )
}

describe('ambient golden particles', () => {
  test('renders a low-density decorative layer that never captures clicks', () => {
    renderParticles()

    const layer = screen.getByTestId('ambient-golden-particles')
    expect(layer).toHaveAttribute('aria-hidden', 'true')
    expect(layer).toHaveStyle({ pointerEvents: 'none' })
    expect(layer.querySelectorAll('.ambient-golden-particle')).toHaveLength(14)
  })

  test('repels nearby particles from a fine pointer without changing layout', async () => {
    renderParticles()

    const layer = screen.getByTestId('ambient-golden-particles')
    fireEvent.pointerMove(window, { pointerType: 'mouse', clientX: 110, clientY: 120 })

    await waitFor(() => {
      const particle = layer.querySelector('.ambient-golden-particle')
      expect(particle.style.getPropertyValue('--repel-x')).not.toBe('0px')
    })
    expect(layer).toHaveAttribute('data-pointer-mode', 'fine')
  })

  test('ignores touch pointers and disables movement when animation is off', async () => {
    writeGuestPreferences({ reduceMotion: 'reduce' })
    renderParticles()

    const layer = screen.getByTestId('ambient-golden-particles')
    expect(layer).toHaveAttribute('data-animation-state', 'static')
    fireEvent.pointerMove(window, { pointerType: 'touch', clientX: 110, clientY: 120 })
    fireEvent.pointerMove(window, { pointerType: 'mouse', clientX: 110, clientY: 120 })

    await waitFor(() => expect(layer).toHaveAttribute('data-pointer-mode', 'disabled'))
    expect(layer.querySelector('.ambient-golden-particle').style.getPropertyValue('--repel-x')).toBe('0px')
  })

  test('is present on both the homepage and auth shell through the shared layout', () => {
    const { unmount } = render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    )
    expect(screen.getByTestId('ambient-golden-particles')).toBeInTheDocument()

    unmount()
    render(
      <MemoryRouter initialEntries={['/login']}>
        <App />
      </MemoryRouter>,
    )
    expect(screen.getByTestId('ambient-golden-particles')).toBeInTheDocument()
  })
})
