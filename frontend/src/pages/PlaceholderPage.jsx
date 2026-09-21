import { useParams } from 'react-router-dom'

const practiceSkills = {
  reading: 'Reading',
  listening: 'Listening',
  writing: 'Writing',
  speaking: 'Speaking',
}

function PlaceholderPage({ title, description }) {
  const { skill } = useParams()
  const skillName = skill ? practiceSkills[skill.toLowerCase()] : null
  const resolvedTitle = skill
    ? skillName
      ? `${skillName} practice is reserved for a later task.`
      : 'Practice area unavailable.'
    : title ?? 'This learning area is reserved for a later task.'
  const resolvedDescription = skill
    ? skillName
      ? 'The practice shell is ready for a future skill implementation.'
      : `The practice skill “${skill}” is not available in this shell.`
    : description ?? 'This route is intentionally kept small for the current task.'

  return (
    <section className="placeholder-page" aria-labelledby="placeholder-title">
      <p className="eyebrow">Coming next</p>
      <h1 id="placeholder-title" className="font-display">
        {resolvedTitle}
      </h1>
      <p className="foundation-copy">{resolvedDescription}</p>
    </section>
  )
}

export default PlaceholderPage
