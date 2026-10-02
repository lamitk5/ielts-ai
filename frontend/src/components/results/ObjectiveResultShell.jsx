import { Link } from 'react-router-dom'
import GlassCard from '../common/GlassCard'
import QuestionResultList from './QuestionResultList'

function ObjectiveResultShell({ result }) {
  const graded = result?.status === 'GRADED'
  const failed = result?.status === 'FAILED'
  const score = typeof result?.score === 'number' ? result.score : null
  const total = typeof result?.total === 'number' ? result.total : null
  return (
    <div className="objective-result-shell">
      <GlassCard className="objective-result-hero" role="status">
        <p className="progress-card-kicker">KẾT QUẢ {result?.skill || 'LUYỆN TẬP'}</p>
        <h2 className="font-display">{graded ? 'Kết quả đã sẵn sàng' : failed ? 'Chưa thể tạo kết quả' : 'Đang xử lý kết quả'}</h2>
        {graded && score !== null && total !== null ? <strong className="objective-result-score">{score}/{total}</strong> : null}
        {graded && result?.accuracy !== null && result?.accuracy !== undefined ? <p>Độ chính xác: {result.accuracy}%</p> : null}
        {!graded && <p>{failed ? 'Bài của bạn vẫn được lưu. Bạn có thể thử tải kết quả lại.' : 'Hệ thống đang hoàn tất phần chấm tự động.'}</p>}
      </GlassCard>
      {graded && <QuestionResultList results={result.questionResults} />}
      <nav className="objective-result-actions" aria-label="Hành động sau kết quả">
        {graded && <Link className="button button-secondary" to={`/practice/${String(result.skill || 'reading').toLowerCase()}`}>Làm bài tương tự</Link>}
        {graded && <Link className="button button-ghost" to="/tutor">Hỏi Én về bài này</Link>}
        <Link className="button button-ghost" to={`/practice/${String(result.skill || 'reading').toLowerCase()}`}>Quay về luyện tập</Link>
      </nav>
    </div>
  )
}

export default ObjectiveResultShell
