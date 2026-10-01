const stateLabels = {
  UNANSWERED: 'Chưa trả lời',
  ANSWERED: 'Đã trả lời',
  REVIEWED: 'Đã xem lại',
}

export function QuestionStateRail({ questions, answers, flaggedIds, reviewedIds, currentQuestionId, onSelect }) {
  const answeredCount = questions.filter((question) => Boolean(answers[question.id])).length

  return (
    <nav
      className="reading-question-rail"
      aria-label="Điều hướng câu hỏi"
      data-state-legend="unanswered-answered-reviewed"
    >
      <p className="reading-question-progress">Đã trả lời {answeredCount}/{questions.length} câu</p>
      <ol className="reading-question-list">
        {questions.map((question, index) => {
          const current = question.id === currentQuestionId
          const flagged = flaggedIds.has(question.id)
          const state = reviewedIds.has(question.id) ? 'REVIEWED' : answers[question.id] ? 'ANSWERED' : 'UNANSWERED'
          const name = `Câu ${index + 1}, ${current ? 'Hiện tại, ' : ''}${stateLabels[state]}${flagged ? ', Đã đánh dấu' : ''}`
          return <li key={question.id}>
            <button
              type="button"
              className="reading-question-jump"
              data-state={state}
              aria-current={current ? 'step' : undefined}
              aria-label={name}
              onClick={() => onSelect(question.id)}
            >
              <strong>Câu {index + 1}</strong>
              {current ? <span className="reading-question-current">Hiện tại</span> : null}
              <span>{stateLabels[state]}</span>
              {flagged ? <span className="reading-question-flag">⚑ Đã đánh dấu</span> : null}
            </button>
          </li>
        })}
      </ol>
    </nav>
  )
}
