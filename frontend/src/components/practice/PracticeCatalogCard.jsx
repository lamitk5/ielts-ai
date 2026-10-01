import GlassCard from '../common/GlassCard'

function PracticeCatalogCard({ skill, title, description, href, active = true }) {
  return (
    <GlassCard className="practice-catalog-card">
      <p className="progress-card-kicker">{skill.toUpperCase()}</p>
      <h2 className="font-display">{skill}</h2>
      <p>{title}</p>
      <span>{description || 'Luyện tập theo lộ trình đã được chuẩn bị.'}</span>
      <a className="button button-secondary button-sm" href={active ? href : '#'} aria-disabled={!active} tabIndex={active ? 0 : -1}>
        {active ? `Mở ${skill}` : `${skill} chưa mở`}
      </a>
    </GlassCard>
  )
}

export default PracticeCatalogCard
