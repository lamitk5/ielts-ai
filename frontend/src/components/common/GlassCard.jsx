function GlassCard({ className = '', interactive = false, children, ...props }) {
  const interactiveClass = interactive ? 'glass-card-interactive' : ''

  return (
    <div
      className={`glass-card ${interactiveClass} ${className}`.trim()}
      {...props}
    >
      {children}
    </div>
  )
}

export default GlassCard
