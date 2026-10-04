import { Link } from 'react-router-dom'
import GlassCard from '../common/GlassCard'

function List({ title, values }) {
  if (!values?.length) return null
  return <div><h3>{title}</h3><ul>{values.map((value, index) => <li key={`${title}-${index}`}>{value}</li>)}</ul></div>
}

function LearnerResultShell({ result }) {
  const skill = String(result?.skill ?? 'reading').toLowerCase()
  const ready = result?.resultStatus === 'READY' || result?.status === 'GRADED' || result?.status === 'COMPLETED'
  const score = result?.score !== null && result?.score !== undefined && result?.total !== null && result?.total !== undefined
    ? `${result.score}/${result.total}` : null
  const evaluation = result?.aiEvaluation
  const review = result?.humanReview
  return (
    <div className="learner-result-shell">
      <GlassCard className="objective-result-hero" role="status">
        <p className="progress-card-kicker">KẾT QUẢ {result?.skill || 'LUYỆN TẬP'}</p>
        <h2 className="font-display">{ready ? 'Kết quả đã sẵn sàng' : result?.resultStatus === 'FAILED' ? 'Chưa thể tạo kết quả' : 'Đang xử lý kết quả'}</h2>
        {score && <strong className="objective-result-score">{score}</strong>}
        {result?.accuracy !== null && result?.accuracy !== undefined && <p>Độ chính xác: {result.accuracy}%</p>}
        {result?.estimatedBand !== null && result?.estimatedBand !== undefined && <p><strong>{result.estimatedBand}</strong> <span>{result.estimatedBandLabel ?? 'Band ước lượng'}</span></p>}
        {result?.availabilityMessage && <p>{result.availabilityMessage}</p>}
        {result?.aiDisclaimer && <p className="text-muted">{result.aiDisclaimer}</p>}
      </GlassCard>
      {evaluation && <GlassCard><h2>Phản hồi AI</h2><List title="Điểm mạnh" values={evaluation.strengths} /><List title="Cần cải thiện" values={evaluation.issues} /><List title="Gợi ý tiếp theo" values={evaluation.suggestions} /></GlassCard>}
      {review && <GlassCard><h2>Nhận xét người chấm</h2><p>{review.feedback || 'Đã có bản nhận xét người chấm.'}</p>{review.overallBand != null && <p>Band ước lượng: {review.overallBand}</p>}</GlassCard>}
      <nav className="objective-result-actions" aria-label="Hành động sau kết quả">
        <Link className="button button-primary" to={`/practice/${skill}`}>Làm bài tương tự</Link>
        <Link className="button button-secondary" to="/tutor">Hỏi Én về bài này</Link>
        <Link className="button button-ghost" to="/practice/history">Xem lịch sử bài làm</Link>
      </nav>
    </div>
  )
}

export default LearnerResultShell
