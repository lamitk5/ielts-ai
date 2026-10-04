import { Link } from 'react-router-dom'
import GlassCard from '../common/GlassCard'

const labels = { IN_PROGRESS: 'Đang làm', PROCESSING: 'Đang xử lý', COMPLETED: 'Hoàn thành', FAILED: 'Chưa hoàn tất' }

export default function SubmissionHistoryList({ items = [] }) {
  if (!items.length) return <GlassCard><p>Chưa có bài đã hoàn thành.</p><Link className="button button-primary" to="/practice">Khám phá bài luyện</Link></GlassCard>
  return <div className="submission-history-list" aria-label="Lịch sử bài làm">{items.map((item) => (
    <GlassCard key={item.id} className="submission-history-item">
      <div><p className="progress-card-kicker">{item.skill}</p><h2>{item.practiceTitle || item.practiceId || 'Bài luyện IELTS'}</h2><p>{labels[item.status] ?? item.status}</p></div>
      <Link className="button button-secondary" to={`/practice/results/${item.id}`}>{item.status === 'COMPLETED' ? 'Xem kết quả' : 'Tiếp tục'}</Link>
    </GlassCard>
  ))}</div>
}
