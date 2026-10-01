function SkeletonBlock({ className = '', label = 'Đang tải' }) {
  return (
    <div
      role="status"
      aria-busy="true"
      className={`skeleton-block ${className}`.trim()}
    >
      <span className="sr-only">{label}</span>
      <div aria-hidden="true" className="skeleton" />
    </div>
  )
}

export default SkeletonBlock
