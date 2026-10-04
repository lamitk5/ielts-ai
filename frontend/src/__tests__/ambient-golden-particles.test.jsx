import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import App from '../App'
import AmbientGoldenParticles from '../components/common/AmbientGoldenParticles'
import { calculateRepulsion, clampVelocity } from '../components/common/particlePhysics'
import { PreferenceProvider } from '../features/preferences/PreferenceProvider'
import { writeGuestPreferences } from '../features/preferences/preferenceStorage'

afterEach(() => {
  cleanup()
  localStorage.clear()
  document.documentElement.removeAttribute('data-reduced-motion')
  vi.restoreAllMocks()
})

beforeEach(() => {
  window.innerWidth = 1440
  window.innerHeight = 900
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
    const particles = layer.querySelectorAll('.ambient-golden-particle')
    expect(particles.length).toBeGreaterThan(32)
    expect(layer.querySelectorAll('[data-particle-layer="dust"]')).not.toHaveLength(0)
    expect(layer.querySelectorAll('[data-particle-layer="glow"]')).not.toHaveLength(0)
    expect(layer.querySelectorAll('[data-particle-layer="spark"]')).not.toHaveLength(0)
    expect([...particles].some((particle) => Number.parseFloat(particle.style.getPropertyValue('--particle-size')) >= 5)).toBe(true)
    expect([...particles].some((particle) => Number.parseFloat(particle.style.getPropertyValue('--particle-opacity')) >= 0.5)).toBe(true)
  })

  test('keeps the intended three-layer weighting and emphasizes the search region', () => {
    renderParticles()

    const layer = screen.getByTestId('ambient-golden-particles')
    const dust = layer.querySelectorAll('[data-particle-layer="dust"]').length
    const glow = layer.querySelectorAll('[data-particle-layer="glow"]').length
    const spark = layer.querySelectorAll('[data-particle-layer="spark"]').length
    const total = dust + glow + spark
    expect(dust / total).toBeGreaterThan(0.5)
    expect(dust / total).toBeLessThan(0.7)
    expect(glow / total).toBeGreaterThan(0.2)
    expect(glow / total).toBeLessThan(0.4)
    expect(spark / total).toBeGreaterThan(0.05)
    expect(spark / total).toBeLessThan(0.2)

    const searchParticles = [...layer.querySelectorAll('[data-particle-region="search"]')]
    expect(searchParticles.length).toBeGreaterThan(0)
    expect(searchParticles.filter((particle) => particle.dataset.particleLayer !== 'dust').length).toBeGreaterThan(0)
  })

  test('reduces particle count on mobile without creating a second engine', () => {
    const desktop = renderParticles()
    const desktopCount = screen.getByTestId('ambient-golden-particles').querySelectorAll('.ambient-golden-particle').length
    desktop.unmount()

    window.innerWidth = 375
    renderParticles()
    const mobileCount = screen.getByTestId('ambient-golden-particles').querySelectorAll('.ambient-golden-particle').length
    expect(mobileCount).toBeLessThan(desktopCount)
    expect(mobileCount).toBeGreaterThan(0)
  })

  test('uses distinct visible token families for dark and light themes', () => {
    writeGuestPreferences({ themeMode: 'dark' })
    const darkRender = renderParticles()
    const darkCore = screen.getByTestId('ambient-golden-particles')
      .querySelector('.ambient-golden-particle')
      .style.getPropertyValue('--particle-core')

    darkRender.unmount()
    writeGuestPreferences({ themeMode: 'light' })
    renderParticles()
    const lightLayer = screen.getByTestId('ambient-golden-particles')
    const lightCore = lightLayer.querySelector('.ambient-golden-particle').style.getPropertyValue('--particle-core')
    expect(lightLayer).toHaveAttribute('data-particle-theme', 'light')
    expect(lightCore).not.toBe(darkCore)
    expect(lightCore).toMatch(/#|rgb|rgba/)
  })

  test('uses a bounded repulsion radius with stronger near force and capped velocity', () => {
    renderParticles()

    const layer = screen.getByTestId('ambient-golden-particles')
    expect(Number(layer.getAttribute('data-repulsion-radius'))).toBeGreaterThanOrEqual(140)
    expect(Number(layer.getAttribute('data-repulsion-radius'))).toBeLessThanOrEqual(170)

    const near = calculateRepulsion(20, 0, 156)
    const far = calculateRepulsion(120, 0, 156)
    const outside = calculateRepulsion(156, 0, 156)
    expect(near.strength).toBeGreaterThan(far.strength)
    expect(far.strength).toBeGreaterThan(0)
    expect(outside.strength).toBe(0)
    expect(Math.hypot(...clampVelocity(10, 10, 2.4))).toBeLessThanOrEqual(2.4)
  })

  test('does not expose portal attraction in the ambient particle task', () => {
    renderParticles()

    const layer = screen.getByTestId('ambient-golden-particles')
    expect(layer).not.toHaveAttribute('data-portal-attraction')
    expect(layer.querySelector('[data-portal-attraction]')).not.toBeInTheDocument()
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

  test('uses a visible light-theme particle treatment without capturing clicks', () => {
    writeGuestPreferences({ themeMode: 'light' })
    renderParticles()

    const layer = screen.getByTestId('ambient-golden-particles')
    expect(layer).toHaveAttribute('data-particle-theme', 'light')
    expect(layer).toHaveStyle({ pointerEvents: 'none' })
    expect(layer.querySelector('.ambient-golden-particle')).toHaveClass('ambient-golden-particle-light')
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
