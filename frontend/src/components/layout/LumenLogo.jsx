function LumenLogo({ compact = false }) {
  return (
    <span className={`lumen-logo ${compact ? 'lumen-logo-compact' : ''}`.trim()}>
      <svg
        className="lumen-logo-mark"
        data-testid="lumen-logo-mark"
        viewBox="0 0 44 44"
        aria-hidden="true"
        focusable="false"
      >
        <path className="lumen-logo-path" d="M10 8v21c0 4 2.8 7 7 7h8" />
        <path className="lumen-logo-path lumen-logo-book" d="M25 14c4 0 6.5 2.1 9 5v15c-2.5-2.4-5-3.6-9-3.6V14Z" />
        <path className="lumen-logo-path lumen-logo-ray" d="M31 8v4M37 11l-3 3M39 18h-4" />
      </svg>
      <span className="lumen-logo-copy">
        <span className="lumen-logo-name">LUMEN IELTS</span>
        <span className="lumen-logo-subtitle">AI Tutor</span>
      </span>
    </span>
  )
}

export default LumenLogo
