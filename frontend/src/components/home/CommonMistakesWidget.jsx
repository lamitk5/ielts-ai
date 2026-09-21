import { AlertCircle } from 'lucide-react'
import GlassCard from '../common/GlassCard'

function CommonMistakesWidget({ mistakes = [] }) {
  return (
    <GlassCard className="common-mistakes-card">
      <div className="progress-card-kicker">
        <AlertCircle aria-hidden="true" size={18} />
        <span>Lỗi thường gặp tuần này</span>
      </div>
      <ul className="common-mistakes-list">
        {mistakes.map((mistake) => (
          <li key={mistake.label}>
            <div className="common-mistake-heading">
              <strong>{mistake.label}</strong>
              <span>{mistake.frequency} lần · {mistake.skill}</span>
            </div>
            <p>{mistake.hint}</p>
          </li>
        ))}
      </ul>
    </GlassCard>
  )
}

export default CommonMistakesWidget
