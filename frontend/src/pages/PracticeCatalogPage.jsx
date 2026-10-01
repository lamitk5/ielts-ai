import { useEffect, useState } from 'react'
import SectionTitle from '../components/common/SectionTitle'
import AnimatedSection from '../components/common/AnimatedSection'
import PracticeCatalogCard from '../components/practice/PracticeCatalogCard'
import PracticeErrorState from '../components/practice/PracticeErrorState'
import { listPracticeCatalog } from '../services/practiceCatalogApi'

const SKILLS = [
  ['reading', 'Reading'], ['listening', 'Listening'], ['writing', 'Writing'], ['speaking', 'Speaking'],
]

function PracticeCatalogPage() {
  const [sets, setSets] = useState([])
  const [error, setError] = useState('')
  const load = () => listPracticeCatalog().then(setSets).catch((err) => setError(err.message))
  useEffect(() => { load() }, [])

  return (
    <section className="practice-catalog-page" aria-labelledby="practice-catalog-title">
      <SectionTitle titleId="practice-catalog-title" eyebrow="PRACTICE LIBRARY" title="Luyện tập theo 4 kỹ năng" description="Chọn một kỹ năng để bắt đầu hoặc tiếp tục lượt luyện tập đã lưu." />
      {error ? <PracticeErrorState message={error} onRetry={load} /> : null}
      <AnimatedSection className="practice-catalog-grid">
        {SKILLS.map(([id, label]) => {
          const item = sets.find((set) => set.skill === id)
          const href = item ? `/practice/${id}/${item.id}` : `/practice/${id}`
          return <PracticeCatalogCard key={id} skill={label} title={item?.title ?? `Khu vực luyện ${label}`} description={item?.description} href={href} active={id === 'writing' || id === 'speaking' || Boolean(item)} />
        })}
      </AnimatedSection>
    </section>
  )
}

export default PracticeCatalogPage
