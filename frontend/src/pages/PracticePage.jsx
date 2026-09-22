import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Button from '../components/common/Button'
import GlassCard from '../components/common/GlassCard'
import PlaceholderPage from './PlaceholderPage'
import { useAuth } from '../features/auth/AuthProvider'
import { practiceFixtures } from '../features/reading/practiceFixtures'
import { fetchPracticeSet, submitPracticeAttempt } from '../features/reading/practiceApi'

function PracticePage() {
  const { skill } = useParams()
  const fixture = practiceFixtures[skill]
  const { isAuthenticated } = useAuth()
  const [practiceSet, setPracticeSet] = useState(fixture)
  const [answers, setAnswers] = useState({})
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!fixture) return undefined
    let active = true
    fetchPracticeSet(skill)
      .then((set) => active && set && setPracticeSet(set))
      .catch(() => {})
    return () => { active = false }
  }, [fixture, skill])

  if (!fixture) {
    return <PlaceholderPage title="Practice area unavailable." description={`The practice skill “${skill}” is not available.`} />
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setResult(null)
    if (!isAuthenticated) {
      setError('Đăng nhập để lưu kết quả luyện tập.')
      return
    }
    try {
      setResult(await submitPracticeAttempt(skill, practiceSet.setId ?? practiceSet.id, answers))
    } catch (submissionError) {
      setError(submissionError.message)
    }
  }

  return (
    <section className="practice-page" aria-labelledby="practice-title">
      <div className="practice-page-header">
        <p className="eyebrow">LUYỆN TẬP {practiceSet.name.toUpperCase()}</p>
        <h1 id="practice-title" className="font-display">{practiceSet.name} practice</h1>
        <p className="foundation-copy">{practiceSet.description}</p>
      </div>
      <form className="practice-form" onSubmit={handleSubmit}>
        {practiceSet.questions.map((question, index) => (
          <GlassCard className="practice-question" key={question.id}>
            <p className="practice-question-number">CÂU {index + 1}</p>
            <h2>{question.prompt}</h2>
            <div className="practice-options" role="radiogroup" aria-label={`Đáp án cho câu ${index + 1}`}>
              {question.options.map((option, optionIndex) => {
                const value = String.fromCharCode(65 + optionIndex)
                return (
                  <label key={option} className="practice-option">
                    <input type="radio" name={question.id} value={value} checked={answers[question.id] === value} onChange={() => setAnswers((current) => ({ ...current, [question.id]: value }))} />
                    <span>{value}. {option}</span>
                  </label>
                )
              })}
            </div>
          </GlassCard>
        ))}
        {error ? <p className="auth-error" role="alert">{error}</p> : null}
        {result ? <GlassCard className="practice-result" role="status"><strong>{result.score}/{result.total}</strong><span>Kết quả đã được lưu trong tiến độ của bạn.</span></GlassCard> : null}
        <div className="practice-actions">
          <Button type="submit" variant="primary" size="lg">Nộp bài</Button>
          <Link className="button button-secondary button-lg" to="/">Về trang chủ</Link>
        </div>
      </form>
    </section>
  )
}

export default PracticePage
