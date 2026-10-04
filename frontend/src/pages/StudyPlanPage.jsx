import { CalendarClock, ChevronRight, Compass } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import GlassCard from '../components/common/GlassCard'
import { getStudyPlan } from '../services/learningEnhancementsApi'

export default function StudyPlanPage() {
  const [plan, setPlan] = useState(null); const [error, setError] = useState('')
  useEffect(() => { getStudyPlan().then(setPlan).catch((cause) => setError(cause.message)) }, [])
  return <section className="learning-page study-plan-page" aria-labelledby="study-plan-title"><div className="search-page-header"><p className="eyebrow">PERSONAL STUDY PLAN</p><h1 id="study-plan-title" className="font-display">Lộ trình học của bạn</h1><p className="foundation-copy">Đề xuất được tính từ mục tiêu và dữ liệu luyện tập đã lưu, không phải lời khuyên ngẫu nhiên.</p></div>
    {error ? <div className="inline-error" role="alert">{error}</div> : null}{!plan ? <p role="status" className="search-status">Đang xây dựng lộ trình…</p> : <><GlassCard className="study-plan-goal"><div><span className="eyebrow">IELTS GOAL</span><strong>{plan.currentEstimatedBand == null ? 'Chưa đủ dữ liệu' : `Band ước lượng ${plan.currentEstimatedBand}`}</strong><span>Mục tiêu: {plan.targetBand == null ? 'Chưa đặt mục tiêu' : `Band ${plan.targetBand}`}</span></div><div><CalendarClock size={20} /><span>{plan.examDate ? `${plan.daysRemaining} ngày còn lại` : 'Chưa có ngày thi'}</span></div></GlassCard><div className="study-plan-list"><h2>Hôm nay nên học gì?</h2>{plan.recommendations?.length ? plan.recommendations.map((item) => <GlassCard key={`${item.skill}-${item.route}`} className="study-plan-item"><div className="study-plan-number">{item.skill === 'VOCABULARY' ? 'V' : item.skill.slice(0, 1)}</div><div><h3>{item.title}</h3><p>{item.reason}</p><small>{item.minutes} phút</small></div><Link to={item.route} aria-label={`Mở ${item.title}`}><ChevronRight size={18} /></Link></GlassCard>) : <GlassCard className="learning-empty-state"><Compass size={28} /><p>Chưa có đề xuất từ dữ liệu luyện tập.</p><Link to="/practice">Bắt đầu một bài luyện</Link></GlassCard>}</div></>}
  </section>
}
