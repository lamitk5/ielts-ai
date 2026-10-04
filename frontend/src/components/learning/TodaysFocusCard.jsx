import { Link } from 'react-router-dom'
import { Target, ArrowRight } from 'lucide-react'
import GlassCard from '../common/GlassCard'

export function TodaysFocusCard({ roadmap }) {
  const hasAction = Boolean(roadmap?.hasRoadmap && roadmap?.primaryAction)
  const actionTitle = hasAction
    ? roadmap.primaryAction.title
    : 'Hoàn thành thêm bài luyện để nhận gợi ý cá nhân hóa'
  const actionRoute = hasAction
    ? roadmap.primaryAction.targetRoute || '/practice/reading'
    : '/practice/reading'
  const buttonLabel = hasAction ? 'Bắt đầu luyện tập' : 'Luyện tập ngay'

  return (
    <GlassCard className="todays-focus-card">
      <div className="todays-focus-header">
        <div className="progress-card-kicker">
          <Target aria-hidden="true" size={18} />
          <span>Trọng tâm hôm nay</span>
        </div>
      </div>
      <div className="todays-focus-body">
        <h4 className="todays-focus-title font-display">{actionTitle}</h4>
        <p className="todays-focus-hint">
          {hasAction
            ? 'Được đề xuất từ tiến độ và dữ liệu phân tích kỹ năng gần nhất của bạn.'
            : 'Hệ thống cần thêm dữ liệu luyện tập để tạo gợi ý trọng tâm chính xác hơn.'}
        </p>
      </div>
      <div className="todays-focus-action">
        <Link className="button btn-liquid button-primary button-md" to={actionRoute}>
          <span className="button-label">{buttonLabel}</span>
          <ArrowRight aria-hidden="true" size={16} />
        </Link>
      </div>
    </GlassCard>
  )
}

export default TodaysFocusCard
