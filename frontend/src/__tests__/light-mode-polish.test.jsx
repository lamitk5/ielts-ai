import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, test } from 'vitest'
import App from '../App'
import { applyPreferenceTokens } from '../features/preferences/PreferenceProvider'
import '../styles/globals.css'

function token(name) {
  return document.documentElement.style.getPropertyValue(name)
}

function luminance(hex) {
  const rgb = hex.match(/[a-f\d]{2}/gi).map((part) => parseInt(part, 16) / 255)
  const linear = rgb.map((channel) => channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4)
  return linear[0] * 0.2126 + linear[1] * 0.7152 + linear[2] * 0.0722
}

function contrast(first, second) {
  const values = [luminance(first), luminance(second)].sort((a, b) => b - a)
  return (values[0] + 0.05) / (values[1] + 0.05)
}

afterEach(() => {
  document.documentElement.removeAttribute('data-theme')
  document.documentElement.removeAttribute('data-density')
  document.documentElement.removeAttribute('data-reduced-motion')
})

describe('Academic Luxury light mode polish', () => {
  test('uses a warm ivory page with layered semantic surfaces', () => {
    applyPreferenceTokens({ themeMode: 'light', accentPreset: 'gold' })

    expect(token('--background')).toBe('#f3ecdf')
    expect(token('--surface')).toBe('#faf5ec')
    expect(token('--surface-alt')).toBe('#eee4d3')
    expect(token('--surface-elevated')).toBe('#fffaf1')
    expect(token('--surface-glass')).toBe('rgba(250, 245, 236, 0.82)')
    expect(token('--surface-raised')).toBe('#fffaf1')
    expect(token('--border-soft')).toBe('rgba(122, 95, 57, 0.2)')
    expect(token('--shadow-soft')).toContain('rgba(74, 56, 31, 0.12)')
    expect(token('--text')).toBe('#10233f')
  })

  test('keeps dark mode semantic palette unchanged while light mode is refined', () => {
    applyPreferenceTokens({ themeMode: 'dark', accentPreset: 'gold' })

    expect(token('--background')).toBe('#060b16')
    expect(token('--surface')).toBe('#0c1424')
    expect(token('--surface-raised')).toBe('#17253d')
    expect(token('--text')).toBe('#f5f7fa')
  })

  test('keeps the bookshelf, learning intelligence card, and LUMEN Scholar in the polished shell', () => {
    applyPreferenceTokens({ themeMode: 'light', accentPreset: 'gold' })
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    )

    expect(document.documentElement).toHaveAttribute('data-theme', 'light')
    expect(screen.getByTestId('hero-bookshelf-background')).toBeInTheDocument()
    expect(screen.getByTestId('hero-visual')).toHaveClass('hero-visual-large')
    expect(screen.getByTestId('lumen-scholar-mascot')).toHaveClass('ai-tutor-pixel-owl-scholar')
  })

  test('keeps accent preferences mapped to the refined semantic controls', () => {
    applyPreferenceTokens({ themeMode: 'light', accentPreset: 'sapphire' })

    expect(token('--accent')).toBe('#255b91')
    expect(token('--accent-soft')).toBe('rgba(80, 136, 198, 0.16)')
    expect(token('--primary-action')).toBe('#4679ad')
    expect(token('--primary-action-text')).toBe('#ffffff')
    expect(token('--selected-border')).toBe('#255b91')
  })

  test('keeps accent hover controls readable on the light surface', () => {
    for (const accentPreset of ['gold', 'sapphire', 'emerald', 'burgundy', 'violet', 'slate']) {
      applyPreferenceTokens({ themeMode: 'light', accentPreset })
      expect(contrast(token('--accent-hover'), token('--surface')), `${accentPreset}/accent hover`).toBeGreaterThanOrEqual(4.5)
    }
  })
})
