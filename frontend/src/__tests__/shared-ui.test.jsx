import { render, screen } from '@testing-library/react'
import { describe, expect, test, vi } from 'vitest'
import AnimatedSection from '../components/common/AnimatedSection'
import Button from '../components/common/Button'
import GlassCard from '../components/common/GlassCard'
import SectionTitle from '../components/common/SectionTitle'
import SkeletonBlock from '../components/common/SkeletonBlock'
import ParallaxLayer from '../components/motion/ParallaxLayer'

function enableReducedMotion() {
  window.matchMedia = vi.fn().mockImplementation(() => ({
    matches: true,
    media: '(prefers-reduced-motion: reduce)',
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }))
}

describe('shared UI primitives', () => {
  test('Button keeps native semantics across supported variants and sizes', () => {
    render(
      <>
        <Button variant="primary" size="sm">Primary</Button>
        <Button variant="secondary" size="md">Secondary</Button>
        <Button variant="ghost" size="lg" disabled className="custom-button">
          Ghost
        </Button>
      </>,
    )

    const primary = screen.getByRole('button', { name: 'Primary' })
    const secondary = screen.getByRole('button', { name: 'Secondary' })
    const ghost = screen.getByRole('button', { name: 'Ghost' })

    primary.focus()
    expect(primary).toHaveFocus()
    expect(primary).toBeEnabled()
    expect(primary).toHaveClass('button-primary', 'button-sm')
    expect(secondary).toHaveClass('button-secondary', 'button-md')
    expect(ghost).toBeDisabled()
    expect(ghost).toHaveClass('button-ghost', 'button-lg', 'custom-button')
  })

  test('GlassCard only adds hover interaction when interactive is true', () => {
    render(
      <>
        <GlassCard data-testid="static-card">Static</GlassCard>
        <GlassCard data-testid="interactive-card" interactive>
          Interactive
        </GlassCard>
      </>,
    )

    expect(screen.getByTestId('static-card')).not.toHaveClass('glass-card-interactive')
    expect(screen.getByTestId('interactive-card')).toHaveClass('glass-card-interactive')
  })

  test('SkeletonBlock exposes a quiet accessible loading state', () => {
    render(<SkeletonBlock label="Đang tải dữ liệu tiến độ" />)

    expect(screen.getByRole('status')).toHaveAttribute('aria-busy', 'true')
    expect(screen.getByText('Đang tải dữ liệu tiến độ')).toHaveClass('sr-only')
    expect(screen.getByRole('status').querySelector('[aria-hidden="true"]')).toBeTruthy()
  })

  test('AnimatedSection keeps content usable when reduced motion is requested', () => {
    enableReducedMotion()
    render(<AnimatedSection>Readable content</AnimatedSection>)

    expect(screen.getByText('Readable content')).toBeVisible()
  })

  test('SectionTitle renders optional eyebrow, title, and description', () => {
    render(
      <SectionTitle
        eyebrow="Foundation"
        title="Shared learning tools"
        description="Reusable components for future study flows."
        align="center"
      />,
    )

    expect(screen.getByText('Foundation')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Shared learning tools' })).toHaveClass(
      'font-display',
    )
    expect(screen.getByText('Reusable components for future study flows.')).toBeInTheDocument()
    expect(screen.getByText('Foundation').parentElement).toHaveClass('section-title-center')
  })

  test('ParallaxLayer renders static usable content with reduced motion', () => {
    enableReducedMotion()
    render(<ParallaxLayer className="decorative-layer">Static layer</ParallaxLayer>)

    const content = screen.getByText('Static layer')
    const layer = content.closest('.parallax-layer')
    expect(content).toBeVisible()
    expect(layer).toHaveClass('decorative-layer')
    expect(layer).not.toHaveAttribute('style')
  })
})
