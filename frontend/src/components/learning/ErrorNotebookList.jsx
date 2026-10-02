import { Link } from 'react-router-dom'
import GlassCard from '../common/GlassCard'

export default function ErrorNotebookList({ entries, onAcknowledge, busyId }) {
  if (!entries.length) return <GlassCard><p>Chưa có lỗi sai nào được ghi nhận từ bài đã nộp.</p><Link to="/practice" className="button button-secondary button-sm">Luyện tập để bắt đầu</Link></GlassCard>
  return <div className="error-notebook-list">{entries.map((entry) => <GlassCard key={entry.id} className="error-notebook-entry"><div className="error-notebook-entry-header"><span className="progress-card-kicker">{entry.skill}</span>{entry.repeated ? <span className="status-chip">Lặp lại {entry.repeatCount} lần</span> : <span className="status-chip">Một quan sát</span>}</div><h2 className="font-display">{entry.mistakeType || 'Lỗi cần xem lại'}</h2><p>{entry.evidence || 'Nguồn bằng chứng sẽ hiển thị khi có dữ liệu phù hợp.'}</p><p className="error-notebook-answer">Câu trả lời: {entry.learnerAnswer || 'Chưa có bản ghi'} · Đáp án tham chiếu: {entry.correctAnswer || 'Chưa có'}</p><button type="button" className="button button-secondary button-sm" disabled={busyId === entry.id} onClick={() => onAcknowledge(entry.id)}>{busyId === entry.id ? 'Đang lưu…' : 'Đánh dấu đã hiểu'}</button></GlassCard>)}</div>
}
