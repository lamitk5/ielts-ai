import { useState } from 'react'
import Button from '../components/common/Button'
import GlassCard from '../components/common/GlassCard'
import { useAuth } from '../features/auth/AuthProvider'
import { saveSpeakingAttempt } from '../features/speaking/speakingApi'
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
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const prompt = prompts.find((item) => item.id === promptId) ?? prompts[0]

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
      <FloatingTutor context={{ skill: 'SPEAKING', exerciseId: promptId }} />
    </section>
  )
}

export default SpeakingPage
