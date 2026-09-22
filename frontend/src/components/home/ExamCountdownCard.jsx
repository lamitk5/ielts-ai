import { CalendarDays } from 'lucide-react'
import GlassCard from '../common/GlassCard'

const DAY_IN_MS = 24 * 60 * 60 * 1000

function getRemainingDays(examDate, now = new Date()) {
  const target = new Date(`${examDate}T00:00:00`)
  const current = new Date(now)

  if (Number.isNaN(target.getTime()) || Number.isNaN(current.getTime())) return 0

  target.setHours(0, 0, 0, 0)
  current.setHours(0, 0, 0, 0)

  return Math.max(0, Math.ceil((target.getTime() - current.getTime()) / DAY_IN_MS))
}

function ExamCountdownCard({ examDate }) {
  if (!examDate) {
    return (
      <GlassCard className="exam-countdown-card">
        <div className="progress-card-kicker">
          <CalendarDays aria-hidden="true" size={18} />
          <span>Ngày thi mục tiêu</span>
        </div>
        <p className="exam-countdown-days">Chưa đặt ngày thi</p>
        <p className="exam-countdown-copy">Bạn có thể thêm ngày thi sau khi sẵn sàng.</p>
      </GlassCard>
    )
  }

  const remainingDays = getRemainingDays(examDate)
  const isPastOrToday = remainingDays === 0

  return (
    <GlassCard className="exam-countdown-card">
      <div className="progress-card-kicker">
        <CalendarDays aria-hidden="true" size={18} />
        <span>Ngày thi mục tiêu</span>
      </div>
      <p className="exam-countdown-days">{remainingDays} ngày</p>
      <p className="exam-countdown-copy">
        {isPastOrToday ? 'Ngày thi đã đến hoặc đã qua' : 'tới kỳ thi IELTS của bạn'}
      </p>
      <time dateTime={examDate}>{examDate}</time>
    </GlassCard>
  )
}

export default ExamCountdownCard
