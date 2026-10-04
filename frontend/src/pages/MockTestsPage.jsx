import { ClipboardCheck, Clock3, ChevronRight } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import GlassCard from '../components/common/GlassCard'
import Button from '../components/common/Button'
import { listMockTestCatalog } from '../services/mockTestApi'

export default function MockTestsPage() {
  const [tests, setTests] = useState(null); const [error, setError] = useState('')
  useEffect(() => { listMockTestCatalog().then(setTests).catch((cause) => setError(cause.message)) }, [])
  return <section className="learning-page mock-tests-page" aria-labelledby="mock-tests-title"><div className="search-page-header"><p className="eyebrow">FULL IELTS MOCK TEST</p><h1 id="mock-tests-title" className="font-display">Thi thử IELTS toàn bài</h1><p className="foundation-copy">Làm Listening, Reading, Writing và Speaking trong một phiên có thời gian thật, lưu tiến độ và xem lại kết quả sau khi nộp.</p></div>{error ? <div className="inline-error" role="alert">{error}</div> : null}{tests == null ? <p role="status" className="search-status">Đang tải danh sách bài thi…</p> : tests.length === 0 ? <GlassCard className="learning-empty-state"><ClipboardCheck size={30} /><p>Hiện chưa có bài thi thử được xuất bản.</p><Link to="/practice">Luyện từng kỹ năng trước</Link></GlassCard> : <div className="vocabulary-grid">{tests.map((test) => <GlassCard key={test.id} className="vocabulary-card"><p className="eyebrow">MOCK TEST</p><h2>{test.title}</h2><p>{test.sections.length} phần thi · {Math.round(test.totalTimeLimitSeconds / 60)} phút</p><span className="status-pill status-mastered"><Clock3 size={13} /> Có thể lưu tiến độ</span><Link to={`/practice/mock-test?mockTestId=${encodeURIComponent(test.slug)}`}><Button>Đọc hướng dẫn <ChevronRight size={16} /></Button></Link></GlassCard>)}</div>}</section>
}
