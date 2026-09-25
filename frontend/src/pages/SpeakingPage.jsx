import { useEffect, useState } from 'react'
import Button from '../components/common/Button'
import GlassCard from '../components/common/GlassCard'
import { useAuth } from '../features/auth/AuthProvider'
import { getSpeakingAttempts, saveSpeakingAttempt } from '../features/speaking/speakingApi'
import FloatingTutor from '../components/tutor/FloatingTutor'

const prompts = [
  { id: 'speaking-p1-01', part: 'PART 1', text: 'Do you enjoy reading in your free time?' },
  { id: 'speaking-p2-01', part: 'PART 2', text: 'Describe a place where you like to study.' },
  { id: 'speaking-p3-01', part: 'PART 3', text: 'How can cities support lifelong learning?' },
]

function SpeakingPage() {
  const { isAuthenticated } = useAuth()
  const [promptId, setPromptId] = useState(prompts[0].id)
  const [transcript, setTranscript] = useState('')
  const [attemptId, setAttemptId] = useState(null)
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [history, setHistory] = useState({ status: 'idle', items: [] })
  const prompt = prompts.find((item) => item.id === promptId) ?? prompts[0]

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

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    if (!isAuthenticated) {
      setError('Đăng nhập để lưu câu trả lời.')
      return
    }
    try {
      const result = await saveSpeakingAttempt(promptId, transcript)
      setStatus(result.status)
      setAttemptId(result.attemptId ?? null)
    } catch (submissionError) {
      setError(submissionError.message)
    }
  }

  return (
    <section className="speaking-page" aria-labelledby="speaking-title">
      <div className="speaking-page-header">
        <p className="eyebrow">LUYỆN TẬP SPEAKING</p>
        <h1 id="speaking-title" className="font-display">Speaking practice</h1>
        <p className="foundation-copy">Luyện ý tưởng và lưu câu trả lời văn bản trong khi lớp STT vẫn được giữ an toàn, minh bạch.</p>
      </div>
      <form className="speaking-form" onSubmit={handleSubmit}>
        <GlassCard className="speaking-prompt-card">
          <label htmlFor="speaking-prompt">Phần thi</label>
          <select id="speaking-prompt" value={promptId} onChange={(event) => setPromptId(event.target.value)}>
            {prompts.map((item) => <option key={item.id} value={item.id}>{item.part}</option>)}
          </select>
          <p className="speaking-part">{prompt.part}</p>
          <h2>{prompt.text}</h2>
        </GlassCard>
        <label htmlFor="speaking-response">Câu trả lời văn bản</label>
        <textarea id="speaking-response" value={transcript} onChange={(event) => setTranscript(event.target.value)} placeholder="Ghi lại ý tưởng hoặc câu trả lời của bạn…" />
        <p className="speaking-boundary">STT chưa được cấu hình — bạn vẫn có thể lưu input văn bản, không tạo transcript hay band giả.</p>
        {error ? <p className="auth-error" role="alert">{error}</p> : null}
        {status ? <GlassCard className="speaking-status" role="status">Trạng thái: {status}</GlassCard> : null}
        <Button type="submit" variant="primary" size="lg">Lưu câu trả lời</Button>
      </form>
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
