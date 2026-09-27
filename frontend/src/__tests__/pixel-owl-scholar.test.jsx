import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, test } from 'vitest'
import FloatingTutor from '../components/tutor/FloatingTutor'

describe('LUMEN Pixel Owl Scholar', () => {
  test('connects bounded global pointer variables to both pupils and the subtle head reaction', async () => {
    render(<FloatingTutor />)
    const launcher = screen.getByRole('button', { name: 'Mở Trợ giảng AI' })
    const mascot = screen.getByTestId('lumen-scholar-mascot')
    const pupils = mascot.querySelectorAll('.ai-tutor-mascot-pupil')
    const face = mascot.querySelector('.ai-tutor-mascot-face')
    launcher.getBoundingClientRect = () => ({ left: 100, top: 100, width: 80, height: 80 })

    expect(pupils).toHaveLength(2)
    pupils.forEach((pupil) => {
      expect(pupil).toHaveStyle({ transform: 'translate(var(--mascot-pupil-x), var(--mascot-pupil-y))' })
    })
    expect(face).toHaveStyle({ transform: 'translate(var(--mascot-head-x), var(--mascot-head-y)) rotate(var(--mascot-head-rotate))' })

    fireEvent.pointerMove(window, { pointerType: 'mouse', clientX: 0, clientY: 140 })
    await waitFor(() => expect(Number.parseFloat(mascot.style.getPropertyValue('--mascot-pupil-x'))).toBeLessThan(0))
    expect(Number.parseFloat(mascot.style.getPropertyValue('--mascot-pupil-x'))).toBeGreaterThanOrEqual(-4)

    fireEvent.pointerMove(window, { pointerType: 'mouse', clientX: 200, clientY: 140 })
    await waitFor(() => expect(Number.parseFloat(mascot.style.getPropertyValue('--mascot-pupil-x'))).toBeGreaterThan(0))
    expect(Number.parseFloat(mascot.style.getPropertyValue('--mascot-pupil-x'))).toBeLessThanOrEqual(4)
  })
})
