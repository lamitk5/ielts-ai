import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, act } from '@testing-library/react'
import { renderHook } from '@testing-library/react'
import { useLocalAudioAmplitude } from '../features/speaking/useLocalAudioAmplitude'
import { LocalAudioVisualizer } from '../components/speaking/LocalAudioVisualizer'
import { MicrophonePermissionState } from '../components/speaking/MicrophonePermissionState'

describe('Task 3: Local Audio Amplitude & Visualizer Boundary', () => {
  let originalMediaDevices
  let originalAudioContext

  beforeEach(() => {
    originalMediaDevices = navigator.mediaDevices
    originalAudioContext = window.AudioContext || window.webkitAudioContext
  })

  afterEach(() => {
    Object.defineProperty(navigator, 'mediaDevices', {
      value: originalMediaDevices,
      writable: true,
      configurable: true,
    })
    if (originalAudioContext) {
      window.AudioContext = originalAudioContext
    }
    vi.restoreAllMocks()
  })

  describe('useLocalAudioAmplitude hook', () => {
    it('handles unavailable mediaDevices gracefully', async () => {
      Object.defineProperty(navigator, 'mediaDevices', {
        value: undefined,
        writable: true,
        configurable: true,
      })

      const onPermissionChange = vi.fn()
      const { result } = renderHook(() =>
        useLocalAudioAmplitude({ isRecording: true, onPermissionChange })
      )

      await act(async () => {
        await result.current.requestPermission()
      })

      expect(result.current.permissionState).toBe('unavailable')
      expect(result.current.amplitude).toBe(0)
      expect(onPermissionChange).toHaveBeenCalledWith('unavailable')
    })

    it('handles microphone permission denial gracefully', async () => {
      const mockGetUserMedia = vi.fn().mockRejectedValue(new Error('Permission denied'))
      Object.defineProperty(navigator, 'mediaDevices', {
        value: { getUserMedia: mockGetUserMedia },
        writable: true,
        configurable: true,
      })

      const onPermissionChange = vi.fn()
      const { result } = renderHook(() =>
        useLocalAudioAmplitude({ isRecording: false, onPermissionChange })
      )

      await act(async () => {
        await result.current.requestPermission()
      })

      expect(result.current.permissionState).toBe('denied')
      expect(result.current.amplitude).toBe(0)
      expect(onPermissionChange).toHaveBeenCalledWith('denied')
    })

    it('initializes local analyser and cleans up tracks on stop/unmount', async () => {
      const stopTrackMock = vi.fn()
      const mockStream = {
        getTracks: () => [{ stop: stopTrackMock }],
      }
      const mockGetUserMedia = vi.fn().mockResolvedValue(mockStream)
      Object.defineProperty(navigator, 'mediaDevices', {
        value: { getUserMedia: mockGetUserMedia },
        writable: true,
        configurable: true,
      })

      const closeContextMock = vi.fn().mockResolvedValue()
      const mockAnalyser = {
        fftSize: 256,
        frequencyBinCount: 128,
        getByteFrequencyData: vi.fn((arr) => {
          arr[0] = 128
          arr[1] = 64
        }),
      }

      class MockAudioContext {
        createMediaStreamSource() {
          return { connect: vi.fn() }
        }
        createAnalyser() {
          return mockAnalyser
        }
        close() {
          return closeContextMock()
        }
      }
      window.AudioContext = MockAudioContext

      const onPermissionChange = vi.fn()
      const { result, unmount } = renderHook(() =>
        useLocalAudioAmplitude({ isRecording: true, onPermissionChange })
      )

      await act(async () => {
        await result.current.requestPermission()
      })

      expect(result.current.permissionState).toBe('granted')
      expect(onPermissionChange).toHaveBeenCalledWith('granted')

      // Unmount should clean up all audio tracks and close context
      unmount()
      expect(stopTrackMock).toHaveBeenCalled()
      expect(closeContextMock).toHaveBeenCalled()
    })

    it('does not send audio or request network endpoints', async () => {
      const fetchSpy = vi.spyOn(globalThis, 'fetch')
      const { result } = renderHook(() => useLocalAudioAmplitude({ isRecording: false }))

      expect(fetchSpy).not.toHaveBeenCalled()
      expect(result.current.amplitude).toBe(0)
    })
  })

  describe('MicrophonePermissionState Component', () => {
    it('renders permission denied message with direct switch to text response', () => {
      const onSwitchToText = vi.fn()
      render(
        <MicrophonePermissionState
          permissionState="denied"
          onSwitchToText={onSwitchToText}
        />
      )

      expect(screen.getByRole('alert')).toBeInTheDocument()
      expect(
        screen.getByText(/Quyền truy cập microphone bị từ chối/i)
      ).toBeInTheDocument()

      const textButton = screen.getByRole('button', { name: /trả lời bằng văn bản/i })
      fireEvent.click(textButton)
      expect(onSwitchToText).toHaveBeenCalledTimes(1)
    })

    it('renders device unavailable message with text fallback', () => {
      render(
        <MicrophonePermissionState
          permissionState="unavailable"
          onSwitchToText={() => {}}
        />
      )

      expect(
        screen.getByText(/Không tìm thấy thiết bị microphone hợp lệ/i)
      ).toBeInTheDocument()
    })
  })

  describe('LocalAudioVisualizer Component', () => {
    it('renders visualizer bars with local notice and respects reduced motion', () => {
      const { container, rerender } = render(
        <LocalAudioVisualizer amplitude={0.65} isRecording={true} reducedMotion={false} />
      )

      expect(
        screen.getByText(/Biểu đồ sóng âm cục bộ/i)
      ).toBeInTheDocument()
      expect(container.querySelectorAll('.visualizer-bar').length).toBeGreaterThan(0)

      // Test reduced motion rendering
      rerender(
        <LocalAudioVisualizer amplitude={0.65} isRecording={true} reducedMotion={true} />
      )
      expect(container.querySelector('.local-visualizer.reduced-motion')).toBeInTheDocument()
    })
  })
})
