import GlassCard from '../common/GlassCard'

function QuestionResultList({ results = [] }) {
  if (!results.length) return <p className="practice-result-empty">Chưa có dữ liệu câu hỏi để xem lại.</p>
  return (
    <section aria-labelledby="question-results-title" className="objective-question-results">
      <h2 id="question-results-title" className="font-display">Xem lại từng câu</h2>
      <div className="objective-question-results-list">
        {results.map((item, index) => (
          <GlassCard key={item.questionId} className={`objective-question-result ${item.correct ? 'is-correct' : 'is-incorrect'}`}>
            <div className="objective-question-result-header">
              <strong>Câu {index + 1}</strong>
              <span>{item.correct ? 'Đúng' : 'Chưa đúng'}</span>
            </div>
            <p>Đáp án của bạn: <b>{item.learnerAnswer || 'Chưa trả lời'}</b></p>
            {!item.correct && <p>Đáp án đúng: <b>{item.correctAnswer}</b></p>}
            {item.explanation && <p className="objective-question-evidence">{item.explanation}</p>}
            {item.evidenceReference && <small>Evidence: {item.evidenceReference}</small>}
          </GlassCard>
        ))}
      </div>
    </section>
  )
}

export default QuestionResultList
