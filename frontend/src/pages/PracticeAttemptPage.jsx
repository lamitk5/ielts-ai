import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import GlassCard from '../components/common/GlassCard'
import PracticeAttemptStatus from '../components/practice/PracticeAttemptStatus'
import PracticeErrorState from '../components/practice/PracticeErrorState'
import { getPracticeSet } from '../services/practiceCatalogApi'
import { saveAttemptAnswers, startAttempt } from '../services/attemptsApi'
import { submitListeningAttempt, submitReadingAttempt } from '../features/reading/practiceApi'

function PracticeAttemptPage() {
  const { skill, setId } = useParams()
  const navigate = useNavigate()
  const [practiceSet, setPracticeSet] = useState(null)
  const [attempt, setAttempt] = useState(null)
  const [answers, setAnswers] = useState({})
  const [error, setError] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const questions = useMemo(() => practiceSet?.questions ?? [], [practiceSet])

  useEffect(() => {
    let active = true
    Promise.all([getPracticeSet(skill, setId), startAttempt({ practiceId: setId, skill })])
      .then(([set, started]) => { if (active) { setPracticeSet(set); setAttempt(started); setAnswers(started.answers ?? {}) } })
      .catch((err) => active && setError(err.message))
    return () => { active = false }
  }, [skill, setId])

  async function choose(questionId, value) {
    const next = { ...answers, [questionId]: value }
    setAnswers(next)
    if (!attempt?.id) return
    try { setAttempt(await saveAttemptAnswers(attempt.id, next)) } catch (err) { setError(err.message) }
  }

  async function submit(event) {
    event.preventDefault()
    if (!attempt?.id) return
    try {
      const submitObjectiveAttempt = String(skill).toLowerCase() === 'listening' ? submitListeningAttempt : submitReadingAttempt
      const idempotencyKey = attempt.idempotencyKey ?? `attempt-${setId}`
      const result = await submitObjectiveAttempt(String(skill).toLowerCase(), setId, attempt.id, answers, idempotencyKey)
      setAttempt(result)
      setSubmitted(true)
      if (result.status === 'FEEDBACK_READY') navigate(`/practice/results/${result.id}`)
    } catch (err) { setError(err.message) }
  }

  if (error) return <PracticeErrorState message={error} />
  if (!practiceSet || !attempt) return <p role="status">Đang chuẩn bị lượt luyện tập…</p>
  return <section className="practice-attempt-page" aria-labelledby="practice-attempt-title">
    <p className="eyebrow">{String(skill).toUpperCase()} · LƯỢT LUYỆN TẬP</p>
    <h1 id="practice-attempt-title" className="font-display">{practiceSet.title ?? practiceSet.name}</h1>
    <PracticeAttemptStatus status={submitted ? attempt.status : 'IN_PROGRESS'} />
    <form onSubmit={submit} className="practice-attempt-form">
      {questions.map((question, index) => <GlassCard key={question.id} className="practice-question"><p className="practice-question-number">CÂU {index + 1}</p><h2>{question.prompt}</h2><div className="practice-options">{question.options.map((option, optionIndex) => { const value = String.fromCharCode(65 + optionIndex); return <label key={value} className="practice-option"><input aria-label={value} type="radio" name={question.id} value={value} checked={answers[question.id] === value} onChange={() => choose(question.id, value)} /><span>{value}. {option}</span></label> })}</div></GlassCard>)}
      <button className="button button-primary" type="submit">Nộp bài</button>
      <Link className="button button-secondary" to={`/practice/${skill}`}>Thoát</Link>
    </form>
  </section>
}

export default PracticeAttemptPage
