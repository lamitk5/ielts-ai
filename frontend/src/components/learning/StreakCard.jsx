import { Flame, CheckCircle2, Circle } from 'lucide-react'
import GlassCard from '../common/GlassCard'

export function StreakCard({ streakInfo = { streakDays: 0, isActiveToday: false } }) {
  const { streakDays = 0, isActiveToday = false } = streakInfo

  const hasStreak = streakDays > 0

  return (
    <GlassCard className="streak-card">
      <div className="progress-card-kicker">
        <Flame aria-hidden="true" size={18} className={hasStreak ? 'streak-flame-active' : ''} />
        <span>Chuỗi học tập</span>
      </div>

      <div className="streak-content">
        <div className="streak-number-display">
          <strong className="streak-digits font-display">{streakDays}</strong>
          <span className="streak-unit">ngày</span>
        </div>

        <p className="streak-message">
          {hasStreak
            ? `${streakDays} ngày liên tiếp có hoạt động luyện tập.`
            : 'Hoàn thành bài luyện tập hôm nay để bắt đầu chuỗi ngày học tập.'}
        </p>

        <div className="streak-status-pill">
          {isActiveToday ? (
            <span className="streak-active-pill">
              <CheckCircle2 size={14} aria-hidden="true" />
              Đã hoàn thành bài hôm nay
            </span>
          ) : (
            <span className="streak-pending-pill">
              <Circle size={14} aria-hidden="true" />
              Chưa có hoạt động hôm nay
            </span>
          )}
        </div>
      </div>
    </GlassCard>
  )
}

export default StreakCard
