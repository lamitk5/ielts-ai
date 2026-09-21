import { useLocation, useParams } from 'react-router-dom'

const practiceSkills = {
  reading: 'Reading',
  listening: 'Listening',
  writing: 'Writing',
  speaking: 'Speaking',
}

function PlaceholderPage({ title, description }) {
  const location = useLocation()
  const { skill } = useParams()
  const skillName = skill ? practiceSkills[skill.toLowerCase()] : null
  const searchQuery = new URLSearchParams(location.search).get('q')?.trim() ?? ''
  const isSearchRoute = location.pathname === '/practice/search'
  const resolvedTitle = isSearchRoute
    ? 'Tìm bài luyện tập'
    : skill
    ? skillName
      ? `${skillName} practice is reserved for a later task.`
      : 'Practice area unavailable.'
    : title ?? 'This learning area is reserved for a later task.'
  const resolvedDescription = isSearchRoute
    ? searchQuery
      ? `Nội dung luyện tập cho “${searchQuery}” sẽ được kết nối trong một task sau.`
      : 'Nhập một chủ đề để tìm bài luyện tập phù hợp trong các task sau.'
    : skill
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
