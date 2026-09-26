import { describe, expect, test, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, act } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { SpeakingTimer } from '../components/speaking/SpeakingTimer'
import { useSpeakingTimer } from '../features/speaking/useSpeakingTimer'
import { SpeakingOrb } from '../components/speaking/SpeakingOrb'

function TimerTestComponent({ initialDuration = 60, mode = 'PREPARATION', onComplete }) {
  const { secondsLeft, isRunning, start, pause, reset } = useSpeakingTimer({
    initialDuration,
    mode,
    onComplete,
  })

  return (
    <div>
      <SpeakingTimer
        secondsLeft={secondsLeft}
        mode={mode}
        isRunning={isRunning}
      />
      <button onClick={start}>Bắt đầu</button>
      <button onClick={pause}>Tạm dừng</button>
      <button onClick={reset}>Đặt lại</button>
    </div>
  )
}

describe('Task 2: Speaking Timers and Orb States', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.useRealTimers()
  })

  test('SpeakingTimer formats minutes and seconds without negative numbers', () => {
    render(<SpeakingTimer secondsLeft={65} mode="PREPARATION" isRunning={true} />)
    expect(screen.getByText('01:05')).toBeInTheDocument()
    expect(screen.getByText(/Thời gian chuẩn bị/i)).toBeInTheDocument()

    // Bounded zero check
    render(<SpeakingTimer secondsLeft={-5} mode="PRACTICE" isRunning={false} />)
    expect(screen.getByText('00:00')).toBeInTheDocument()
    expect(screen.getByText(/Thời gian luyện nói/i)).toBeInTheDocument()
  })

  test('useSpeakingTimer counts down and fires onComplete when reaching zero', () => {
    const onComplete = vi.fn()
    render(<TimerTestComponent initialDuration={3} mode="PREPARATION" onComplete={onComplete} />)

    expect(screen.getByText('00:03')).toBeInTheDocument()

    // Start timer
    act(() => {
      screen.getByText('Bắt đầu').click()
    })

    // Advance 2 seconds
    act(() => {
      vi.advanceTimersByTime(2000)
    })
    expect(screen.getByText('00:01')).toBeInTheDocument()
    expect(onComplete).not.toHaveBeenCalled()

    // Advance 1 second -> reach 0
    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByText('00:00')).toBeInTheDocument()
    expect(onComplete).toHaveBeenCalledTimes(1)
  })

  test('useSpeakingTimer pause and reset controls work as expected', () => {
    render(<TimerTestComponent initialDuration={10} mode="PREPARATION" />)

    act(() => {
      screen.getByText('Bắt đầu').click()
    })
    act(() => {
      vi.advanceTimersByTime(3000)
    })
    expect(screen.getByText('00:07')).toBeInTheDocument()

    // Pause
    act(() => {
      screen.getByText('Tạm dừng').click()
    })
    act(() => {
      vi.advanceTimersByTime(3000)
    })
    // Time should remain paused at 00:07
    expect(screen.getByText('00:07')).toBeInTheDocument()

    // Reset
    act(() => {
      screen.getByText('Đặt lại').click()
    })
    expect(screen.getByText('00:10')).toBeInTheDocument()
  })

  test('SpeakingOrb respects reducedMotion prop and removes pulse animation classes', () => {
    const { container, rerender } = render(<SpeakingOrb state="RECORDING_LOCAL" reducedMotion={false} />)
    const wrapper = container.querySelector('.speaking-orb-wrapper')
    expect(wrapper).not.toHaveClass('reduced-motion')

    rerender(<SpeakingOrb state="RECORDING_LOCAL" reducedMotion={true} />)
    expect(wrapper).toHaveClass('reduced-motion')
  })
})
