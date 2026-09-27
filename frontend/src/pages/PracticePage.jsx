import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Button from '../components/common/Button'
import GlassCard from '../components/common/GlassCard'
import PlaceholderPage from './PlaceholderPage'
import { useAuth } from '../features/auth/AuthProvider'
import { practiceFixtures } from '../features/reading/practiceFixtures'
import {
  fetchPracticeSet,
  savePracticeAnswers,
  startPracticeAttempt,
  submitListeningAttempt,
  submitPracticeAttempt,
  submitReadingAttempt,
} from '../features/reading/practiceApi'
import FloatingTutor from '../components/tutor/FloatingTutor'
import { SplitLearningWorkspace } from '../components/workspace/SplitLearningWorkspace'
import { ReadingPassagePane } from '../components/workspace/ReadingPassagePane'
import { ReadingQuestionPane } from '../components/workspace/ReadingQuestionPane'
import { saveSessionSnapshot, loadSessionSnapshot } from '../features/session/sessionStorage'
import { validateSessionSnapshot } from '../features/session/sessionContinuity'
import { usePreferences } from '../features/preferences/PreferenceProvider'
import ListeningPracticeWorkspace from '../components/listening/ListeningPracticeWorkspace'

function PracticeSkillSession({ skill, fixture }) {
  const [practiceSet, setPracticeSet] = useState(fixture)

  useEffect(() => {
    let active = true
    fetchPracticeSet(skill)
      .then((set) => { if (active && set) setPracticeSet(set) })
      .catch(() => {})
    return () => { active = false }
  }, [skill])

  const setId = practiceSet.setId ?? practiceSet.id
  const questionIdentity = practiceSet.questions?.map(({ id, prompt, options }) => [id, prompt, options]) ?? []
  return <PracticeSetSession key={JSON.stringify([setId, questionIdentity])} skill={skill} practiceSet={practiceSet} />
}

function PracticeSetSession({ skill, practiceSet }) {
  const { user, isAuthenticated } = useAuth()
  const { preferences } = usePreferences()
  const setId = practiceSet.setId ?? practiceSet.id
  const questions = practiceSet.questions ?? []

  const [answers, setAnswers] = useState({})
  const [currentQuestionId, setCurrentQuestionId] = useState(() => {
    if (user?.id) {
      const loaded = loadSessionSnapshot(user.id)
      const validated = validateSessionSnapshot(loaded, { validSetIds: [setId] })
      if (validated?.currentQuestionId && questions.some((q) => q.id === validated.currentQuestionId)) {
        return validated.currentQuestionId
      }
    }
    return practiceSet.questions?.[0]?.id ?? null
  })
  const [result, setResult] = useState(null)
  const [durableAttempt, setDurableAttempt] = useState(null)
  const [error, setError] = useState('')
  const [flaggedIds, setFlaggedIds] = useState(() => new Set())
  const [reviewedIds, setReviewedIds] = useState(() => new Set())
  const [remainingSeconds, setRemainingSeconds] = useState(60 * 60)
  const [timerExpired, setTimerExpired] = useState(false)

  useEffect(() => {
    if (!isAuthenticated || !['reading', 'listening'].includes(skill)) return undefined
    let active = true
    const idempotencyKey = `${skill}:${setId}`
    startPracticeAttempt(skill, setId, `${setId}:v1`, idempotencyKey)
      .then((attempt) => {
        if (!active || !attempt?.id) return
        setDurableAttempt(attempt)
        if (attempt.answers && Object.keys(attempt.answers).length) setAnswers(attempt.answers)
      })
      .catch(() => {})
    return () => { active = false }
  }, [isAuthenticated, skill, setId])

  useEffect(() => {
    if (skill !== 'reading' || !preferences.timerDefaultEnabled) return undefined
    const startedAt = durableAttempt?.startedAt ? Date.parse(durableAttempt.startedAt) : Date.now()
    const update = () => {
      const next = Math.max(0, 60 * 60 - Math.floor((Date.now() - startedAt) / 1000))
      setRemainingSeconds(next)
      setTimerExpired(next === 0)
    }
    update()
    const timer = window.setInterval(update, 1000)
    return () => window.clearInterval(timer)
  }, [durableAttempt?.startedAt, preferences.timerDefaultEnabled, skill])

  const activeQuestionId = questions.some((question) => question.id === currentQuestionId)
    ? currentQuestionId
    : questions[0]?.id ?? null

  const handleSelectQuestion = (newId) => {
    setCurrentQuestionId(newId)
    if (user?.id) {
      saveSessionSnapshot(user.id, {
        skill,
        setId,
        currentQuestionId: newId,
      })
    }
  }

  const toggleId = (setter, id) => setter((current) => {
    const next = new Set(current)
    if (next.has(id)) next.delete(id)
    else next.add(id)
    return next
  })

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setResult(null)
    if (!isAuthenticated) {
      setError('Đăng nhập để lưu kết quả luyện tập.')
      return
    }
    if (timerExpired) {
      setError('Thời gian luyện tập đã hết. Hãy xem lại kết quả đã lưu của bạn.')
      return
    }
    try {
      if (skill === 'reading' && durableAttempt?.id) {
        const submitted = await submitReadingAttempt(skill, setId, durableAttempt.id, answers, `${skill}:${setId}`)
        setResult({ ...submitted, attemptId: submitted.id })
      } else if (skill === 'listening' && durableAttempt?.id) {
        const submitted = await submitListeningAttempt(skill, setId, durableAttempt.id, answers, `${skill}:${setId}`)
        setResult({ ...submitted, attemptId: submitted.id })
      } else {
        setResult(await submitPracticeAttempt(skill, setId, answers))
      }
    } catch (submissionError) {
      setError(submissionError.message)
    }
  }

  return (
    <section className={`practice-page practice-page-editorial${skill === 'reading' ? ' practice-page-reading' : ''}`} aria-labelledby="practice-title">
      <div className="practice-page-header">
        <p className="eyebrow">LUYỆN TẬP {practiceSet.name.toUpperCase()}</p>
        <h1 id="practice-title" className="font-display">{practiceSet.name} practice</h1>
        <p className="foundation-copy">{practiceSet.description}</p>
      </div>
      <form className="practice-form" onSubmit={handleSubmit}>
        {skill === 'reading' && preferences.timerDefaultEnabled ? (
          <div className="practice-timer" role="timer" aria-label="Thời gian còn lại">
            <span>THỜI GIAN CÒN LẠI</span>
            <strong>{timerExpired ? 'Hết giờ' : `${String(Math.floor(remainingSeconds / 60)).padStart(2, '0')}:${String(remainingSeconds % 60).padStart(2, '0')}`}</strong>
          </div>
        ) : null}
        {skill === 'listening' ? (
          <ListeningPracticeWorkspace
            questions={questions}
            answers={answers}
            onAnswer={(id, value) => {
              handleSelectQuestion(id)
              setAnswers((current) => {
                const next = { ...current, [id]: value }
                if (durableAttempt?.id) void savePracticeAnswers(durableAttempt.id, next).catch(() => {})
                return next
              })
            }}
          />
        ) : null}
        {skill === 'reading' ? (
          <SplitLearningWorkspace
            workspace="reading"
            leftLabel="Nội dung"
            rightLabel="Câu hỏi"
            left={<ReadingPassagePane practiceSet={practiceSet} />}
            right={
              <ReadingQuestionPane
                questions={questions}
                currentQuestionId={activeQuestionId}
                answers={answers}
                flaggedIds={flaggedIds}
                reviewedIds={reviewedIds}
                onSelect={handleSelectQuestion}
                onAnswer={(id, value) => {
                  handleSelectQuestion(id)
                  setAnswers((current) => {
                    const next = { ...current, [id]: value }
                    if (durableAttempt?.id) void savePracticeAnswers(durableAttempt.id, next).catch(() => {})
                    return next
                  })
                  setReviewedIds((current) => {
                    const next = new Set(current)
                    next.delete(id)
                    return next
                  })
                  setResult(null)
                }}
                onToggleFlag={(id) => toggleId(setFlaggedIds, id)}
                onToggleReviewed={(id) => toggleId(setReviewedIds, id)}
              />
            }
          />
        ) : skill === 'listening' ? null : questions.map((question, index) => (
          <GlassCard className="practice-question" key={question.id}>
            <p className="practice-question-number">CÂU {index + 1}</p>
            <h2>{question.prompt}</h2>
            <div className="practice-options" role="radiogroup" aria-label={`Đáp án cho câu ${index + 1}`}>
              {question.options.map((option, optionIndex) => {
                const value = String.fromCharCode(65 + optionIndex)
                return (
                  <label key={option} className="practice-option">
                    <input
                      type="radio"
                      name={question.id}
                      value={value}
                      checked={answers[question.id] === value}
                      onChange={() => {
                        handleSelectQuestion(question.id)
                        setAnswers((current) => {
                          const next = { ...current, [question.id]: value }
                          if (durableAttempt?.id) void savePracticeAnswers(durableAttempt.id, next).catch(() => {})
                          return next
                        })
                      }}
                    />
                    <span>{value}. {option}</span>
                  </label>
                )
              })}
            </div>
          </GlassCard>
        ))}
        {error ? <p className="auth-error" role="alert">{error}</p> : null}
        {result ? (
          <GlassCard className="practice-result" role="status">
            <strong>{result.score}/{result.total}</strong>
            <span>Kết quả đã được lưu trong tiến độ của bạn.</span>
          </GlassCard>
        ) : null}
        <div className="practice-actions">
          <Button type="submit" variant="primary" size="lg" disabled={timerExpired}>Nộp bài</Button>
          <Link className="button button-secondary button-lg" to="/">Về trang chủ</Link>
        </div>
      </form>
      <FloatingTutor
        context={{
          skill: skill.toUpperCase(),
          lessonId: practiceSet.setId ?? practiceSet.id,
          exerciseId: practiceSet.setId ?? practiceSet.id,
          questionId: activeQuestionId,
          ...(result?.attemptId || result?.id ? { attemptId: result.attemptId ?? result.id } : {}),
        }}
      />
    </section>
  )
}

function PracticePage() {
  const { skill } = useParams()
  const fixture = practiceFixtures[skill]
  if (!fixture) {
    return <PlaceholderPage title="Practice area unavailable." description={`The practice skill “${skill}” is not available.`} />
  }
  return <PracticeSkillSession key={skill} skill={skill} fixture={fixture} />
}

export default PracticePage
