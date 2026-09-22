import { useState } from 'react'
import Button from '../components/common/Button'
import GlassCard from '../components/common/GlassCard'
import { useAuth } from '../features/auth/AuthProvider'
import { submitWriting } from '../features/writing/writingApi'

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
  const selectedTask = tasks.find((task) => task.id === taskId) ?? tasks[0]
  const wordCount = responseText.trim() ? responseText.trim().split(/\s+/).length : 0

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
    </section>
  )
}

export default WritingPage
