import { useEffect, useRef } from 'react'
import { QuestionStateRail } from './QuestionStateRail'

export function ReadingQuestionPane({ questions, currentQuestionId, answers, flaggedIds, reviewedIds, onSelect, onAnswer, onToggleFlag, onToggleReviewed }) {
  const currentIndex = Math.max(0, questions.findIndex((question) => question.id === currentQuestionId))
  const question = questions[currentIndex]
  const headingRef = useRef(null)
  const previousQuestionId = useRef(currentQuestionId)

  useEffect(() => {
    if (currentQuestionId !== previousQuestionId.current) headingRef.current?.focus()
    previousQuestionId.current = currentQuestionId
  }, [currentQuestionId])

  if (!question) return <p>Chưa có câu hỏi trong bộ đề này.</p>

  const flagged = flaggedIds.has(question.id)
  const reviewed = reviewedIds.has(question.id)

  return (
    <div className="reading-questions">
      <QuestionStateRail questions={questions} answers={answers} flaggedIds={flaggedIds} reviewedIds={reviewedIds} currentQuestionId={question.id} onSelect={onSelect} />
      <article className="reading-current-question" data-reading-target-id={question.id}>
        <div className="reading-question-heading">
          <p className="practice-question-number">CÂU {currentIndex + 1} / {questions.length}</p>
          <h2 ref={headingRef} tabIndex={-1}>{question.prompt}</h2>
        </div>
        <div className="practice-options" role="radiogroup" aria-label={`Đáp án cho câu ${currentIndex + 1}`}>
          {question.options.map((option, optionIndex) => {
            const value = String.fromCharCode(65 + optionIndex)
            return <label key={value} className="practice-option">
              <input type="radio" name={question.id} value={value} checked={answers[question.id] === value} onChange={() => onAnswer(question.id, value)} />
              <span>{value}. {option}</span>
            </label>
          })}
        </div>
        <div className="reading-question-tools">
          <button type="button" aria-pressed={flagged} onClick={() => onToggleFlag(question.id)}>{flagged ? 'Bỏ đánh dấu câu hỏi' : 'Đánh dấu câu hỏi'}</button>
          <button type="button" aria-pressed={reviewed} onClick={() => onToggleReviewed(question.id)}>{reviewed ? 'Bỏ trạng thái đã xem lại' : 'Đánh dấu đã xem lại'}</button>
        </div>
        <div className="reading-question-navigation" aria-label="Chuyển câu hỏi">
          <button type="button" disabled={currentIndex === 0} onClick={() => onSelect(questions[currentIndex - 1].id)}>Câu trước</button>
          <button type="button" disabled={currentIndex === questions.length - 1} onClick={() => onSelect(questions[currentIndex + 1].id)}>Câu tiếp</button>
        </div>
      </article>
    </div>
  )
}
