import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import PracticeErrorState from '../components/practice/PracticeErrorState'
import PracticeResultSummary from '../components/practice/PracticeResultSummary'
import ObjectiveResultShell from '../components/results/ObjectiveResultShell'
import LearnerResultShell from '../components/results/LearnerResultShell'
import { getAttempt, getObjectiveResult } from '../services/attemptsApi'
import { getLearnerResult } from '../services/resultsApi'

function PracticeResultPage() {
  const { attemptId } = useParams()
  const [attempt, setAttempt] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => {
    getLearnerResult(attemptId).catch(() => getObjectiveResult(attemptId)).catch(() => getAttempt(attemptId)).then(setAttempt).catch((err) => setError(err.message))
  }, [attemptId])
  if (error) return <PracticeErrorState message={error} />
  if (!attempt) return <p role="status">Đang tải kết quả…</p>
  return <section className="practice-result-page" aria-labelledby="practice-result-title"><p className="eyebrow">KẾT QUẢ LUYỆN TẬP</p><h1 id="practice-result-title" className="font-display">Lượt luyện tập đã hoàn tất</h1>{attempt.resultStatus ? <LearnerResultShell result={attempt} /> : Array.isArray(attempt.questionResults) ? <ObjectiveResultShell result={attempt} /> : <><PracticeResultSummary attempt={attempt} /><Link className="button button-primary" to={`/practice/${attempt.skill}`}>Làm lại</Link></>}</section>
}

export default PracticeResultPage
