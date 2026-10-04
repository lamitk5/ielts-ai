function SectionTitle({ eyebrow, title, description, align = 'left', titleId }) {
  const alignmentClass = align === 'center' ? 'section-title-center' : ''

  return (
    <div className={`section-title ${alignmentClass}`.trim()}>
      {eyebrow ? <p className="eyebrow section-title-eyebrow">{eyebrow}</p> : null}
      <h2 id={titleId} className="font-display">{title}</h2>
      {description ? <p className="section-title-description">{description}</p> : null}
    </div>
  )
}

export default SectionTitle
