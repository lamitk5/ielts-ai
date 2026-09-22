import { useEffect, useState } from 'react'
import Button from '../components/common/Button'
import GlassCard from '../components/common/GlassCard'
import { useAuth } from '../features/auth/AuthProvider'
import { getWritingSubmissions, submitWriting } from '../features/writing/writingApi'
import FloatingTutor from '../components/tutor/FloatingTutor'

const tasks = [
  { id: 'task-1-academic-01', label: 'Task 1 · Academic', prompt: 'Summarise the information in a chart or process.', minimumWords: 150 },
  { id: 'task-2-opinion-01', label: 'Task 2 · Essay', prompt: 'Discuss both views and give your own opinion.', minimumWords: 250 },
]

function WritingPage() {
  const { isAuthenticated } = useAuth()
  const [taskId, setTaskId] = useState(tasks[0].id)
  const [responseText, setResponseText] = useState('')
  const [assessment, setAssessment] = useState(null)
  const [error, setError] = useState('')
  const [history, setHistory] = useState({ status: 'idle', items: [] })
  const selectedTask = tasks.find((task) => task.id === taskId) ?? tasks[0]
  const wordCount = responseText.trim() ? responseText.trim().split(/\s+/).length : 0

  useEffect(() => {
    if (!isAuthenticated) {
      setHistory({ status: 'idle', items: [] })
      return undefined
    }
    let active = true
    setHistory({ status: 'loading', items: [] })
    getWritingSubmissions()
      .then((items) => active && setHistory({ status: 'ready', items: Array.isArray(items) ? items : [] }))
      .catch(() => active && setHistory({ status: 'error', items: [] }))
    return () => { active = false }
  }, [isAuthenticated])

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setAssessment(null)
    if (!isAuthenticated) {
      setError('Đăng nhập để lưu và nhận đánh giá bài viết.')
      return
    }
    if (wordCount < 20) {
      setError('Hãy viết thêm nội dung trước khi gửi.')
      return
    }
    try {
      setAssessment(await submitWriting(taskId, responseText))
    } catch (submissionError) {
      setError(submissionError.message)
    }
  }

  return (
    <section className="writing-page" aria-labelledby="writing-title">
      <div className="writing-page-header">
        <p className="eyebrow">LUYỆN TẬP WRITING</p>
        <h1 id="writing-title" className="font-display">Writing practice</h1>
        <p className="foundation-copy">Viết theo đề Task 1 hoặc Task 2, sau đó nhận phản hồi có giới hạn rõ ràng từ hệ thống.</p>
      </div>
      <form className="writing-form" onSubmit={handleSubmit}>
        <GlassCard className="writing-prompt-card">
          <label htmlFor="writing-task">Chọn dạng bài</label>
          <select id="writing-task" value={taskId} onChange={(event) => setTaskId(event.target.value)}>
            {tasks.map((task) => <option key={task.id} value={task.id}>{task.label}</option>)}
          </select>
          <h2>{selectedTask.prompt}</h2>
          <p>Tối thiểu {selectedTask.minimumWords} từ · hiện có {wordCount} từ</p>
        </GlassCard>
        <label className="sr-only" htmlFor="writing-response">Bài viết</label>
        <textarea id="writing-response" aria-label="Bài viết" value={responseText} onChange={(event) => setResponseText(event.target.value)} placeholder="Bắt đầu viết bài của bạn…" />
        <p className="writing-boundary">Band ước lượng sẽ chỉ xuất hiện khi đánh giá AI trả về dữ liệu hợp lệ; đây không phải điểm thi chính thức.</p>
        {error ? <p className="auth-error" role="alert">{error}</p> : null}
        {assessment ? (
          <GlassCard className="writing-assessment" role="status">
            {assessment.overallBandEstimate == null ? <strong>Chưa khả dụng</strong> : <strong>Band ước lượng {assessment.overallBandEstimate}</strong>}
            <span>{assessment.disclaimer}</span>
          </GlassCard>
        ) : null}
        <Button type="submit" variant="primary" size="lg">Gửi bài viết</Button>
      </form>
      {isAuthenticated ? (
        <GlassCard className="practice-history" aria-labelledby="writing-history-title">
          <div className="practice-history-heading">
            <div>
              <p className="progress-card-kicker">BÀI ĐÃ LƯU</p>
              <h2 id="writing-history-title" className="font-display">Lịch sử Writing</h2>
            </div>
          </div>
          {history.status === 'loading' ? <p className="practice-history-muted">Đang tải lịch sử…</p> : null}
          {history.status === 'error' ? <p className="practice-history-muted" role="status">Chưa thể tải lịch sử lúc này.</p> : null}
          {history.status === 'ready' && history.items.length === 0 ? <p className="practice-history-muted">Chưa có bài viết đã lưu.</p> : null}
          {history.status === 'ready' && history.items.length > 0 ? (
            <ul className="practice-history-list">
              {history.items.map((item, index) => (
                <li key={`${item.taskId}-${item.createdAt ?? index}`}>
                  <div>
                    <strong>{item.taskId?.startsWith('task-2') ? 'Task 2 · Essay' : 'Task 1 · Academic'}</strong>
                    <span>{item.wordCount ?? 0} từ</span>
                  </div>
                  <span>{item.overallBandEstimate == null ? 'Chưa có band ước lượng' : `Band ước lượng ${item.overallBandEstimate}`}</span>
                </li>
              ))}
            </ul>
          ) : null}
        </GlassCard>
      ) : null}
      <FloatingTutor context={{ skill: 'WRITING', exerciseId: taskId, taskType: selectedTask.label }} />
    </section>
  )
}

export default WritingPage
