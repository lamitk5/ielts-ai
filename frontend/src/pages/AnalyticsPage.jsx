import { Link } from 'react-router-dom'
import { BarChart3, TrendingDown } from 'lucide-react'
import { useEffect, useState } from 'react'
import GlassCard from '../components/common/GlassCard'
import { getLearningAnalytics } from '../services/learningEnhancementsApi'

export default function AnalyticsPage() {
  const [analytics, setAnalytics] = useState(null); const [error, setError] = useState('')
  useEffect(() => { getLearningAnalytics().then(setAnalytics).catch((cause) => setError(cause.message)) }, [])
  return <section className="learning-page analytics-page" aria-labelledby="analytics-title"><div className="search-page-header"><p className="eyebrow">LEARNING ANALYTICS</p><h1 id="analytics-title" className="font-display">Tiến độ &amp; phân tích</h1><p className="foundation-copy">Chỉ hiển thị những gì đã có từ kết quả luyện tập thực tế của bạn.</p></div>{error ? <div className="inline-error" role="alert">{error}</div> : null}{!analytics ? <p role="status" className="search-status">Đang tải phân tích…</p> : analytics.hasEvidence ? <><div className="analytics-skill-grid">{analytics.skills.map((item) => <GlassCard key={item.skill}><p className="eyebrow">{item.skill}</p><strong>{item.accuracyPercent == null ? 'Chưa đủ dữ liệu' : `${Math.round(item.accuracyPercent)}% chính xác`}</strong><span>{item.completedAttempts} lượt hoàn thành</span></GlassCard>)}</div><GlassCard className="analytics-question-types"><h2>Hiệu suất theo dạng câu hỏi</h2>{analytics.questionTypes.map((item) => <div className="analytics-row" key={item.questionType}><span>{item.questionType}</span><strong>{Math.round(item.accuracyPercent)}%</strong><div className="analytics-bar"><span style={{ width: `${item.accuracyPercent}%` }} /></div></div>)}<p className="analytics-callout"><TrendingDown size={16} /> Điểm cần chú ý: {analytics.weakestArea ?? 'Chưa xác định'}</p></GlassCard></> : <GlassCard className="learning-empty-state"><BarChart3 size={32} /><h2>Chưa đủ dữ liệu để phân tích</h2><p>Hoàn thành thêm bài luyện để mở khóa xu hướng.</p><Link to="/practice">Luyện tập ngay</Link></GlassCard>}</section>
}
