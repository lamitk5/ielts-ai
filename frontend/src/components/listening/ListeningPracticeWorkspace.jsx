import GlassCard from '../common/GlassCard'

export function ListeningPracticeWorkspace({ questions = [], answers = {}, onAnswer }) {
  return (
    <div className="listening-practice-workspace">
      <GlassCard className="practice-boundary listening-media-boundary" role="status">
        <strong>Phát audio chưa được cấu hình</strong>
        <span>Bộ đề này chưa có media được phê duyệt. Không hiển thị nội dung nghe hay kết quả giả.</span>
      </GlassCard>
      {questions.map((question, index) => (
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
                    onChange={() => onAnswer?.(question.id, value)}
                  />
                  <span>{value}. {option}</span>
                </label>
              )
            })}
          </div>
        </GlassCard>
      ))}
    </div>
  )
}

export default ListeningPracticeWorkspace
