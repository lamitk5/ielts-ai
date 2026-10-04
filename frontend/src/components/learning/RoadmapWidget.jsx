import { MapPin, Milestone } from 'lucide-react'
import GlassCard from '../common/GlassCard'

export function RoadmapWidget({ roadmap }) {
  const hasRoadmap = Boolean(roadmap?.hasRoadmap)
  const currentMilestone = hasRoadmap
    ? roadmap.currentMilestone
    : 'Chưa có lộ trình (Cần hoàn thành bài đánh giá ban đầu)'
  const nextMilestone = hasRoadmap ? roadmap.nextMilestone : null
  const percent = hasRoadmap ? Math.min(100, Math.max(0, roadmap.completionPercent || 0)) : 0

  return (
    <GlassCard className="roadmap-widget-card">
      <div className="progress-card-kicker">
        <Milestone aria-hidden="true" size={18} />
        <span>Lộ trình cá nhân</span>
      </div>

      <div className="roadmap-milestone-current">
        <div className="roadmap-milestone-marker">
          <MapPin size={16} aria-hidden="true" />
          <strong>Mục tiêu hiện tại:</strong>
        </div>
        <p className="roadmap-milestone-text">{currentMilestone}</p>
      </div>

      {nextMilestone ? (
        <div className="roadmap-milestone-next">
          <span className="roadmap-next-label">Cột mốc tiếp theo:</span>
          <p className="roadmap-next-text">{nextMilestone}</p>
        </div>
      ) : null}

      <div className="roadmap-progress-bar-container">
        <div className="roadmap-progress-header">
          <span>Tiến độ hoàn thành</span>
          <strong>{percent}%</strong>
        </div>
        <div className="roadmap-progress-track">
          <div
            className="roadmap-progress-fill"
            style={{ width: `${percent}%` }}
            role="progressbar"
            aria-valuenow={percent}
            aria-valuemin={0}
            aria-valuemax={100}
          />
        </div>
      </div>
    </GlassCard>
  )
}

export default RoadmapWidget
