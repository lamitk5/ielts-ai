import { ArrowRight, BookOpen, Headphones, Mic2, PenLine } from 'lucide-react'
import { Link } from 'react-router-dom'
import GlassCard from '../common/GlassCard'

const skillIcons = {
  reading: BookOpen,
  listening: Headphones,
  writing: PenLine,
  speaking: Mic2,
}

function SkillCard({ skill, index }) {
  const Icon = skillIcons[skill.id] ?? BookOpen
  const headingId = `skill-card-${skill.id}-title`

  return (
    <GlassCard
      className="skill-card skill-card-editorial"
      interactive
      role="article"
      aria-labelledby={headingId}
    >
      <div className="skill-card-topline">
        <span className="skill-card-number">{String(index + 1).padStart(2, '0')}</span>
        <span className="skill-card-icon" aria-hidden="true">
          <Icon size={22} strokeWidth={1.6} />
        </span>
      </div>
      <h3 id={headingId} className="font-display">
        {skill.name}
      </h3>
      <p className="skill-card-description">{skill.description}</p>
      <div className="skill-card-footer">
        <p className="skill-card-metric">{skill.metric}</p>
        <Link className="skill-card-action" to={`/practice/${skill.id}`}>
          Luyện tập
          <ArrowRight aria-hidden="true" size={16} />
        </Link>
      </div>
      <span className="skill-card-orbit" aria-hidden="true" />
    </GlassCard>
  )
}

export default SkillCard
