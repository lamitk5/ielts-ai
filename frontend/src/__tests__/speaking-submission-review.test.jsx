import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import SpeakingResultShell from '../components/results/SpeakingResultShell.jsx'
import {
  uploadSpeakingAudio,
  getSpeakingAudioUrl,
  getSpeakingSubmissionReview,
  submitSpeakingReview,
} from '../features/speaking/speakingApi.js'

describe('SpeakingResultShell Component', () => {
  it('renders pending review / ungraded state correctly with safe disclaimer', () => {
    const submission = {
      transcript: 'I enjoy studying in quiet libraries because it helps me concentrate.',
      transcriptSource: 'MANUAL',
      status: 'SUBMITTED',
    }

    render(<SpeakingResultShell submission={submission} review={null} />)

    expect(screen.getByText(/Đang chờ chấm \/ Chưa có đánh giá/i)).toBeInTheDocument()
    expect(screen.getByText(/không tự động tạo điểm phát âm hay STT giả/i)).toBeInTheDocument()
    expect(screen.getByText(/I enjoy studying in quiet libraries because it helps me concentrate\./i)).toBeInTheDocument()
    expect(screen.getByText(/Nguồn: MANUAL/i)).toBeInTheDocument()
  })

  it('renders graded review with 4 criteria, overall band, feedback, and disclaimer', () => {
    const submission = {
      transcript: 'Lifelong learning enables citizens to adapt to technological changes.',
      transcriptSource: 'MANUAL',
      status: 'GRADED',
    }
    const review = {
      overallBand: 7.5,
      fluencyCoherence: 7.5,
      lexicalResource: 8.0,
      grammaticalRange: 7.0,
      pronunciation: 6.5,
      reviewerFeedback: 'Good natural flow and academic vocabulary.',
      status: 'COMPLETED',
    }

    render(<SpeakingResultShell submission={submission} review={review} />)

    expect(screen.getByText(/Band Overall:/i)).toBeInTheDocument()
    expect(screen.getAllByText('7.5').length).toBe(2)
    expect(screen.getByText(/Fluency & Coherence/i)).toBeInTheDocument()
    expect(screen.getByText('8.0')).toBeInTheDocument()
    expect(screen.getByText('7.0')).toBeInTheDocument()
    expect(screen.getByText('6.5')).toBeInTheDocument()
    expect(screen.getByText('7.0')).toBeInTheDocument()
    expect(screen.getByText(/Good natural flow and academic vocabulary\./i)).toBeInTheDocument()
    expect(screen.getByText(/Đánh giá bởi giám khảo chuyên môn — Không phải điểm thi IELTS chính thức\./i)).toBeInTheDocument()
  })

  it('renders audio player when audioUrl is provided', () => {
    const submission = { transcript: 'Test audio transcript', transcriptSource: 'MANUAL' }
    render(
      <SpeakingResultShell
        submission={submission}
        audioUrl="/api/practice/speaking/submissions/123/audio"
      />
    )

    const audioEl = screen.getByTestId('speaking-audio-player')
    expect(audioEl).toBeInTheDocument()
    expect(audioEl).toHaveAttribute('src', '/api/practice/speaking/submissions/123/audio')
  })

  it('renders error message when error is provided', () => {
    render(<SpeakingResultShell error={{ message: 'Không thể kết nối máy chủ' }} />)
    expect(screen.getByText(/Không thể tải đánh giá Speaking/i)).toBeInTheDocument()
    expect(screen.getByText(/Không thể kết nối máy chủ/i)).toBeInTheDocument()
  })
})

describe('Speaking API Review and Audio Methods', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('constructs correct audio streaming URL', () => {
    const url = getSpeakingAudioUrl('sub-123')
    expect(url).toBe('/api/practice/speaking/submissions/sub-123/audio')
  })

  it('uploads audio using FormData and auth header', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'test-jwt' }))
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ storageKey: 'audio-key-1.webm', audioSizeBytes: 1024 }),
    })
    global.fetch = mockFetch

    const blob = new Blob(['audio data'], { type: 'audio/webm' })
    const result = await uploadSpeakingAudio('sub-123', blob)

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/practice/speaking/submissions/sub-123/audio',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          Authorization: 'Bearer test-jwt',
        }),
      })
    )
    expect(result.storageKey).toBe('audio-key-1.webm')
  })

  it('fetches speaking review', async () => {
    const mockReview = { overallBand: 7.0, status: 'COMPLETED' }
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => mockReview,
    })
    global.fetch = mockFetch

    const result = await getSpeakingSubmissionReview('sub-123')
    expect(mockFetch).toHaveBeenCalledWith('/api/practice/speaking/submissions/sub-123/review', expect.any(Object))
    expect(result.overallBand).toBe(7.0)
  })

  it('submits speaking review', async () => {
    const mockReview = { overallBand: 8.0, status: 'COMPLETED' }
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => mockReview,
    })
    global.fetch = mockFetch

    const result = await submitSpeakingReview('sub-123', { overallBand: 8.0 })
    expect(mockFetch).toHaveBeenCalledWith(
      '/api/practice/speaking/submissions/sub-123/review',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
      })
    )
    expect(result.overallBand).toBe(8.0)
  })
})
