import { useEffect, useState } from 'react'
import GlassCard from '../components/common/GlassCard'
import { useAuth } from '../features/auth/AuthProvider'
import { getSpeakingAttempts, saveSpeakingAttempt } from '../features/speaking/speakingApi'
import FloatingTutor from '../components/tutor/FloatingTutor'
import SpeakingRoom from '../components/speaking/SpeakingRoom'

const prompts = [
  { id: 'speaking-p1-01', part: 'PART 1', text: 'Do you enjoy reading in your free time?' },
  { id: 'speaking-p2-01', part: 'PART 2', text: 'Describe a place where you like to study.', points: ['Where it is', 'How often you go there'] },
  { id: 'speaking-p3-01', part: 'PART 3', text: 'How can cities support lifelong learning?' },
]

function SpeakingPage() {
  const { isAuthenticated } = useAuth()
  const [promptId, setPromptId] = useState(prompts[0].id)
  const [attemptId, setAttemptId] = useState(null)
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [history, setHistory] = useState({ status: 'idle', items: [] })

  useEffect(() => {
    if (!isAuthenticated) {
      setHistory({ status: 'idle', items: [] })
      return undefined
    }
    let active = true
    setHistory({ status: 'loading', items: [] })
    getSpeakingAttempts()
      .then((items) => active && setHistory({ status: 'ready', items: Array.isArray(items) ? items : [] }))
      .catch(() => active && setHistory({ status: 'error', items: [] }))
    return () => { active = false }
  }, [isAuthenticated])

  async function handleSaveTranscript(selectedId, transcriptText) {
    setError('')
    setStatus('')
    if (!isAuthenticated) {
      setError('Đăng nhập để lưu câu trả lời.')
      return
    }
    setSubmitting(true)
    try {
      const result = await saveSpeakingAttempt(selectedId, transcriptText)
      setStatus(result.status)
      setAttemptId(result.attemptId ?? null)
    } catch (submissionError) {
      setError(submissionError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="speaking-page speaking-page-editorial" aria-labelledby="speaking-title">
      <div className="speaking-page-header">
        <p className="eyebrow">LUYỆN TẬP SPEAKING</p>
        <h1 id="speaking-title" className="font-display">Speaking practice</h1>
        <p className="foundation-copy">Luyện ý tưởng và lưu câu trả lời văn bản trong khi lớp STT vẫn được giữ an toàn, minh bạch.</p>
      </div>

      <SpeakingRoom
        prompts={prompts}
        selectedPromptId={promptId}
        onSelectPrompt={setPromptId}
        onSaveTranscript={handleSaveTranscript}
        submitting={submitting}
        statusMessage={status}
        errorMessage={error}
      />

      {isAuthenticated ? (
        <GlassCard className="practice-history" aria-labelledby="speaking-history-title">
          <div className="practice-history-heading">
            <div>
              <p className="progress-card-kicker">CÂU TRẢ LỜI ĐÃ LƯU</p>
              <h2 id="speaking-history-title" className="font-display">Lịch sử Speaking</h2>
            </div>
          </div>
          {history.status === 'loading' ? <p className="practice-history-muted">Đang tải lịch sử…</p> : null}
          {history.status === 'error' ? <p className="practice-history-muted" role="status">Chưa thể tải lịch sử lúc này.</p> : null}
          {history.status === 'ready' && history.items.length === 0 ? <p className="practice-history-muted">Chưa có câu trả lời đã lưu.</p> : null}
          {history.status === 'ready' && history.items.length > 0 ? (
            <ul className="practice-history-list">
              {history.items.map((item, index) => (
                <li key={`${item.promptId}-${item.createdAt ?? index}`}>
                  <div>
                    <strong>{item.promptId?.includes('p2') ? 'Part 2' : item.promptId?.includes('p3') ? 'Part 3' : 'Part 1'}</strong>
                    <span>{item.transcript ? `${item.transcript.length} ký tự` : 'Input văn bản trống'}</span>
                  </div>
                  <span>{item.status ?? 'Đã lưu'}</span>
                </li>
              ))}
            </ul>
          ) : null}
        </GlassCard>
      ) : null}

      <FloatingTutor context={{
        skill: 'SPEAKING',
        exerciseId: promptId,
        promptId,
        ...(attemptId ? { attemptId } : {}),
      }} />
    </section>
  )
}

export default SpeakingPage
