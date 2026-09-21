function SourceChip({ citation }) {
  return (
    <span className="tutor-source-chip">
      {citation.title}
      <span aria-hidden="true"> · {citation.section}</span>
    </span>
  )
}

export default SourceChip
