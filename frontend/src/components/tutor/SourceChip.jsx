function SourceChip({ citation }) {
  const details = [
    citation.section,
    citation.page != null ? `p. ${citation.page}` : null,
  ].filter(Boolean)

  return (
    <span className="tutor-source-chip" aria-label={`Nguồn: ${citation.title}${details.length ? ` · ${details.join(' · ')}` : ''}`}>
      {citation.title}
      {details.length ? <span aria-hidden="true"> · {details.join(' · ')}</span> : null}
    </span>
  )
}

export default SourceChip
