import { useEffect, useRef, useState } from 'react'
import GlassCard from '../components/common/GlassCard'
import { useAuth } from '../features/auth/AuthProvider'
import { getWritingSubmissions, submitWriting } from '../features/writing/writingApi'
import { getCurrentDraft, saveDraft, deleteDraft } from '../services/learningDraftsApi'
import FloatingTutor from '../components/tutor/FloatingTutor'
import { SplitLearningWorkspace } from '../components/workspace/SplitLearningWorkspace'
import { WritingPromptPane } from '../components/workspace/WritingPromptPane'
import { WritingEditorPane } from '../components/workspace/WritingEditorPane'

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
  const [submitting, setSubmitting] = useState(false)
  const [history, setHistory] = useState({ status: 'idle', items: [] })

  // Draft persistence state
  const [draftId, setDraftId] = useState(null)
  const [draftVersion, setDraftVersion] = useState(0)
  const [saveStatus, setSaveStatus] = useState('idle')
  const saveTimeoutRef = useRef(null)
  const latestTextRef = useRef(responseText)
  const draftVersionRef = useRef(draftVersion)

  latestTextRef.current = responseText
  draftVersionRef.current = draftVersion

  // Practice timer state (optional learning aid)
  const [timerEnabled, setTimerEnabled] = useState(false)
  const [timerSeconds, setTimerSeconds] = useState(0)
  const timerRef = useRef(null)

  const selectedTask = tasks.find((task) => task.id === taskId) ?? tasks[0]
  const wordCount = responseText.trim() ? responseText.trim().split(/\s+/).length : 0

  useEffect(() => {
    if (timerEnabled) {
      timerRef.current = setInterval(() => {
        setTimerSeconds((prev) => prev + 1)
      }, 1000)
    } else if (timerRef.current) {
      clearInterval(timerRef.current)
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [timerEnabled])

  // Hydrate draft on mount or taskId change
  useEffect(() => {
    if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current)
    if (!isAuthenticated) {
      setResponseText('')
      setDraftId(null)
      setDraftVersion(0)
      setSaveStatus('idle')
      return undefined
    }

    let active = true
    getCurrentDraft('WRITING', taskId)
      .then((draft) => {
        if (!active) return
        if (draft && draft.contentSnapshot) {
          setResponseText(draft.contentSnapshot)
          setDraftId(draft.id)
          setDraftVersion(draft.version ?? 1)
          setSaveStatus('saved')
        } else {
          setResponseText('')
          setDraftId(null)
          setDraftVersion(0)
          setSaveStatus('idle')
        }
      })
      .catch(() => {
        if (active) setSaveStatus('idle')
      })

    return () => {
      active = false
      if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current)
    }
  }, [isAuthenticated, taskId])

  // Fetch history for authenticated members
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

  const handleTextChange = (nextText) => {
    setResponseText(nextText)
    if (!isAuthenticated) {
      setSaveStatus('idle')
      return
    }

    setSaveStatus('dirty')
    if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current)

    saveTimeoutRef.current = setTimeout(async () => {
      const textToSave = latestTextRef.current
      if (!textToSave.trim()) return

      setSaveStatus('saving')
      try {
        const result = await saveDraft({
          skill: 'WRITING',
          referenceId: taskId,
          contentSnapshot: textToSave,
          expectedVersion: draftVersionRef.current > 0 ? draftVersionRef.current : undefined,
        })
        setDraftId(result.id)
        setDraftVersion(result.version)
        setSaveStatus('saved')
      } catch (err) {
        if (err.code === 'VERSION_CONFLICT') {
          setSaveStatus('conflict')
        } else {
          setSaveStatus('save_failed')
        }
      }
    }, 400)
  }

  async function handleSubmit(event) {
    if (event?.preventDefault) event.preventDefault()
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
    setSubmitting(true)
    try {
      const result = await submitWriting(taskId, responseText)
      setAssessment(result)
      if (draftId) {
        deleteDraft(draftId).catch(() => {})
        setDraftId(null)
        setDraftVersion(0)
      }
    } catch (submissionError) {
      setError(submissionError.message)
    } finally {
      setSubmitting(false)
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
        <SplitLearningWorkspace
          workspace="writing"
          leftLabel="Đề bài"
          rightLabel="Bài viết"
          left={
            <WritingPromptPane
              tasks={tasks}
              selectedTaskId={taskId}
              selectedTask={selectedTask}
              onSelectTask={(id) => {
                setTaskId(id)
                setAssessment(null)
                setError('')
              }}
              wordCount={wordCount}
            />
          }
          right={
            <WritingEditorPane
              value={responseText}
              onChange={handleTextChange}
              wordCount={wordCount}
              minWords={selectedTask.minimumWords}
              saveStatus={saveStatus}
              timerEnabled={timerEnabled}
              timerSeconds={timerSeconds}
              onToggleTimer={() => setTimerEnabled((prev) => !prev)}
              error={error}
              assessment={assessment}
              submitting={submitting}
              onSubmit={handleSubmit}
            />
          }
        />
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

      <FloatingTutor context={{ skill: 'WRITING', exerciseId: taskId, taskType: selectedTask.label, draftVersion }} />
    </section>
  )
}

export default WritingPage
