import GlassCard from '../common/GlassCard'

export default function DiagnosticResult({ result }) {
  if (!result) return null
  return (
    <GlassCard className="diagnostic-result" aria-labelledby="diagnostic-result-title">
      <p className="eyebrow">HỒ SƠ KHỞI ĐIỂM</p>
      <h2 id="diagnostic-result-title" className="font-display">{result.title || 'Estimated starting profile'}</h2>
      <p>{result.disclaimer}</p>
      <div className="diagnostic-result-list">
        {(result.sections || []).map((section) => (
          <div key={section.skill} className="diagnostic-result-row">
            <strong>{section.skill}</strong>
            <span>{section.state === 'READY' ? `${section.estimatedBand ?? '—'} · Band ước lượng` : 'Insufficient evidence'}</span>
          </div>
        ))}
      </div>
    </GlassCard>
  )
}
