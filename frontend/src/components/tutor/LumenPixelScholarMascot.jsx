function PixelEye({ side, blinking }) {
  const eyeClass = `ai-tutor-mascot-eye ai-tutor-mascot-eye-${side}`
  const eyeX = side === 'left' ? 40 : 68
  const pupilX = side === 'left' ? 47 : 75

  return (
    <g className={eyeClass}>
      {blinking ? (
        <rect className="pixel-scholar-eye-line" x={eyeX} y="59" width="20" height="4" fill="var(--navy)" />
      ) : (
        <>
          <rect className="pixel-scholar-eye-white" x={eyeX} y="54" width="20" height="14" fill="#fffaf0" />
      <rect
        className="ai-tutor-mascot-pupil pixel-scholar-pupil"
        data-testid={`mascot-pupil-${side}`}
        x={pupilX}
        y="58"
        width="6"
        height="6"
        fill="var(--navy)"
        style={{ transform: 'translate(var(--mascot-pupil-x), var(--mascot-pupil-y))' }}
      />
        </>
      )}
    </g>
  )
}

export default function LumenPixelScholarMascot({ prefersReducedMotion, isBlinking = false }) {
  return (
    <svg
      className="ai-tutor-mascot ai-tutor-pixel-scholar"
      data-testid="lumen-scholar-mascot"
      data-idle-motion={prefersReducedMotion ? 'disabled' : 'enabled'}
      data-blink={isBlinking ? 'closed' : 'open'}
      viewBox="0 0 128 128"
      shapeRendering="crispEdges"
      role="img"
      aria-label="LUMEN Pixel Scholar"
      focusable="false"
    >
      <rect className="ai-tutor-mascot-aura" x="8" y="8" width="112" height="112" fill="var(--accent-soft)" opacity=".3" />
      <g
        className="ai-tutor-mascot-face"
        style={{ transform: 'translate(var(--mascot-head-x), var(--mascot-head-y)) rotate(var(--mascot-head-rotate))' }}
      >
        <path className="ai-tutor-mascot-head" d="M32 28h8v-8h48v8h8v8h8v48h-8v8H88v8H40v-8h-8v-8h-8V36h8Z" fill="var(--navy)" stroke="var(--accent)" strokeWidth="2" />
        <path className="pixel-scholar-face-plate" d="M36 44h8v-8h40v8h8v32h-8v8H44v-8h-8Z" fill="#f5ead1" />
        <path className="ai-tutor-mascot-cap" d="M24 40h8v-8h8v-8h48v8h8v8h8v8H24Z" fill="var(--navy)" stroke="var(--gold-light)" strokeWidth="2" />
        <rect className="pixel-scholar-cap-band" x="32" y="42" width="64" height="4" fill="var(--gold-light)" />
        <rect className="pixel-scholar-brow" x="40" y="50" width="20" height="3" fill="var(--accent)" />
        <rect className="pixel-scholar-brow" x="68" y="50" width="20" height="3" fill="var(--accent)" />
        <PixelEye side="left" blinking={isBlinking} />
        <PixelEye side="right" blinking={isBlinking} />
        <rect className="pixel-scholar-mouth" x="56" y="76" width="16" height="4" fill="var(--gold-light)" />
      </g>
      <path className="ai-tutor-mascot-shoulders" d="M20 124v-12h8v-8h16v-8h8v-8h24v8h8v8h16v8h8v12Z" fill="var(--navy)" stroke="var(--accent)" strokeWidth="2" />
      <rect className="pixel-scholar-robe-highlight" x="60" y="96" width="8" height="28" fill="var(--gold-light)" opacity=".9" />
    </svg>
  )
}
