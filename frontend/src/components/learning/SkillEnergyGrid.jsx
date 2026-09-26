import { Activity } from 'lucide-react'
import GlassCard from '../common/GlassCard'

const DEFAULT_SKILLS = [
  { skill: 'reading', name: 'Reading', band: null, bandLabel: 'Chưa đủ dữ liệu', hasSufficientData: false },
  { skill: 'listening', name: 'Listening', band: null, bandLabel: 'Chưa đủ dữ liệu', hasSufficientData: false },
  { skill: 'writing', name: 'Writing', band: null, bandLabel: 'Chưa đủ dữ liệu', hasSufficientData: false },
  { skill: 'speaking', name: 'Speaking', band: null, bandLabel: 'Chưa đủ dữ liệu', hasSufficientData: false },
]

export function SkillEnergyGrid({ skills = [] }) {
  const displaySkills = DEFAULT_SKILLS.map((defaultItem) => {
    const found = Array.isArray(skills)
      ? skills.find((s) => s.skill?.toLowerCase() === defaultItem.skill.toLowerCase())
      : null
    return found || defaultItem
  })

  return (
    <GlassCard className="skill-energy-card">
      <div className="progress-card-kicker">
        <Activity aria-hidden="true" size={18} />
        <span>Năng lượng & Độ vững kỹ năng</span>
      </div>

      <div className="skill-energy-grid">
        {displaySkills.map((item) => {
          const band = typeof item.band === 'number' ? item.band : null
          const percent = band !== null ? Math.min(100, Math.max(10, (band / 9) * 100)) : 10
          const hasData = item.hasSufficientData || band !== null

          return (
            <div key={item.skill} className="skill-energy-item">
              <div className="skill-energy-header">
                <strong className="skill-energy-name">{item.name}</strong>
                <span className={`skill-energy-label ${hasData ? 'has-data' : 'empty'}`}>
                  {item.bandLabel || (hasData ? `Band ước lượng ${band?.toFixed(1)}` : 'Chưa đủ dữ liệu')}
                </span>
              </div>
              <div className="skill-energy-track">
                <div
                  className={`skill-energy-fill ${hasData ? 'filled' : 'insufficient'}`}
                  style={{ width: `${percent}%` }}
                  role="progressbar"
                  aria-valuenow={band !== null ? band : 0}
                  aria-valuemin={0}
                  aria-valuemax={9}
                  aria-label={`Độ vững ${item.name}`}
                />
              </div>
            </div>
          )
        })}
      </div>
    </GlassCard>
  )
}

export default SkillEnergyGrid
