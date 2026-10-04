import GlassCard from '../common/GlassCard'

function PracticeResultSummary({ attempt }) {
  return (
    <GlassCard className="practice-result-summary" role="status">
      <p className="progress-card-kicker">KẾT QUẢ ĐÃ LƯU</p>
      <strong>{attempt.score ?? 0}/{attempt.total ?? 0}</strong>
      <span>{attempt.skill ? `Kỹ năng ${attempt.skill}` : 'Lượt luyện tập'}</span>
      <small>Phiên bản {attempt.practiceVersion || 'v1'}</small>
    </GlassCard>
  )
}

export default PracticeResultSummary
