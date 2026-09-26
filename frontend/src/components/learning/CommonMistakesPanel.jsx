import { AlertCircle, ArrowUpRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import GlassCard from '../common/GlassCard'

const SKILL_LABELS = {
  reading: 'Reading',
  listening: 'Listening',
  writing: 'Writing',
  speaking: 'Speaking',
  general: 'Chung',
}

const SKILL_ROUTES = {
  reading: '/practice/reading',
  listening: '/practice/listening',
  writing: '/practice/writing',
  speaking: '/speaking',
  general: '/practice/reading',
}

export function CommonMistakesPanel({
  mistakes = [],
  title = 'Lỗi thường gặp tuần này',
}) {
  const hasMistakes = Array.isArray(mistakes) && mistakes.length > 0

  return (
    <GlassCard className="common-mistakes-card">
      <div className="progress-card-kicker">
        <AlertCircle aria-hidden="true" size={18} />
        <span>{title}</span>
      </div>

      {!hasMistakes ? (
        <div className="common-mistakes-empty">
          <p>
            Chưa ghi nhận lỗi sai cần chú ý. Tiếp tục luyện tập để theo dõi điểm cần cải thiện.
          </p>
        </div>
      ) : (
        <ul className="common-mistakes-list">
          {mistakes.map((mistake) => {
            const skillKey = (mistake.skill || 'general').toLowerCase()
            const skillName = SKILL_LABELS[skillKey] || mistake.skill
            const targetRoute = SKILL_ROUTES[skillKey] || '/practice/reading'

            return (
              <li key={mistake.id || mistake.issue || mistake.label}>
                <div className="common-mistake-heading">
                  <strong>{mistake.issue || mistake.label}</strong>
                  <span>
                    {mistake.count ? `${mistake.count} lần · ` : mistake.frequency ? `${mistake.frequency} lần · ` : ''}
                    {skillName}
                  </span>
                </div>
                {mistake.recommendation || mistake.hint ? (
                  <p>{mistake.recommendation || mistake.hint}</p>
                ) : null}
                <div className="common-mistake-link-wrap">
                  <Link to={targetRoute} className="common-mistake-link">
                    <span>Luyện dạng bài này</span>
                    <ArrowUpRight size={14} aria-hidden="true" />
                  </Link>
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </GlassCard>
  )
}

export default CommonMistakesPanel
