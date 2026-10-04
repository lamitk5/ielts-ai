import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import GlassCard from '../common/GlassCard'
import SkeletonBlock from '../common/SkeletonBlock'
import { getTodaysPlan } from '../../services/todaysPlanApi'

export default function TodaysPlanSection({ enabled = true }) {
  const [state, setState] = useState({ status: enabled ? 'loading' : 'idle', data: null, error: '' })
  useEffect(() => {
    if (!enabled) return
    let active = true
    getTodaysPlan().then((data) => active && setState({ status: 'ready', data, error: '' })).catch((cause) => active && setState({ status: 'error', data: null, error: cause.message }))
    return () => { active = false }
  }, [enabled])
  if (!enabled) return null
  if (state.status === 'loading') return <GlassCard className="todays-plan-section"><SkeletonBlock label="Đang tải kế hoạch hôm nay" /></GlassCard>
  if (state.status === 'error') return <GlassCard className="todays-plan-section" role="alert"><p>{state.error}</p><Link to="/practice" className="button button-secondary button-sm">Mở danh mục bài luyện</Link></GlassCard>
  const items = (state.data?.items || []).slice(0, 5)
  return <section className="todays-plan-section" aria-labelledby="todays-plan-title"><div className="section-title"><p className="eyebrow">KẾ HOẠCH HÔM NAY</p><h2 id="todays-plan-title" className="font-display">Hôm nay học gì?</h2></div>{items.length ? <div className="todays-plan-grid">{items.map((item) => <GlassCard key={item.id} className="todays-plan-card"><span className="progress-card-kicker">{item.skill}</span><h3 className="font-display">{item.title}</h3><p>{item.reason}</p><small>{item.durationMinutes} phút · {item.targeted ? 'Theo dữ liệu luyện tập' : 'Gợi ý bắt đầu'}</small><Link to={item.route} className="button button-secondary button-sm">Bắt đầu</Link></GlassCard>)}</div> : <GlassCard><p>{state.data?.message || 'Chưa có kế hoạch hôm nay.'}</p><Link to="/practice" className="button button-secondary button-sm">Chọn bài luyện</Link></GlassCard>}</section>
}
