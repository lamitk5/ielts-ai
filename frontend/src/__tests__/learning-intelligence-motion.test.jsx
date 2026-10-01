import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, test, vi } from 'vitest'
import HeroVisual from '../components/home/HeroVisual'

const motionPreference = vi.hoisted(() => ({ reducedMotion: false }))

vi.mock('../features/preferences/PreferenceProvider', async () => {
  const actual = await vi.importActual('../features/preferences/PreferenceProvider')
  return {
    ...actual,
    useOptionalPreferences: () => ({ reducedMotion: motionPreference.reducedMotion }),
    useEffectiveReducedMotion: () => motionPreference.reducedMotion,
  }
})

afterEach(() => {
  motionPreference.reducedMotion = false
})

describe('Learning Intelligence motion polish', () => {
  test('keeps the existing four-skill card and exposes calm motion layers', () => {
    render(<HeroVisual />)

    const visual = screen.getByTestId('hero-visual')

    expect(visual).toHaveAttribute('data-hero-visual', 'learning-intelligence')
    expect(visual).toHaveClass('hero-visual-motion-enabled')
    expect(visual.querySelector('.hero-orbit-motion')).toBeInTheDocument()
    expect(visual.querySelector('.hero-core-motion')).toBeInTheDocument()
    expect(visual.querySelectorAll('.hero-signal')).toHaveLength(4)
    expect(screen.getByText('R')).toBeInTheDocument()
    expect(screen.getByText('L')).toBeInTheDocument()
    expect(screen.getByText('W')).toBeInTheDocument()
    expect(screen.getByText('S')).toBeInTheDocument()
  })

  test('suppresses decorative motion when the shared animation preference is off', () => {
    motionPreference.reducedMotion = true

    render(<HeroVisual />)

    const visual = screen.getByTestId('hero-visual')
    expect(visual).toHaveClass('hero-visual-motion-disabled')
    expect(visual).toHaveAttribute('data-motion-state', 'static')
  })

  test('applies and resets subtle pointer parallax on the visualization card', () => {
    render(<HeroVisual />)

    const card = document.querySelector('.hero-intelligence-card')
    Object.defineProperty(card, 'getBoundingClientRect', {
      configurable: true,
      value: () => ({ left: 100, top: 100, width: 320, height: 240 }),
    })

    fireEvent.pointerMove(card, { clientX: 400, clientY: 100 })
    expect(card.style.getPropertyValue('--hero-tilt-y')).not.toBe('0deg')

    fireEvent.pointerLeave(card)
    expect(card.style.getPropertyValue('--hero-tilt-x')).toBe('0deg')
    expect(card.style.getPropertyValue('--hero-tilt-y')).toBe('0deg')
  })
})
