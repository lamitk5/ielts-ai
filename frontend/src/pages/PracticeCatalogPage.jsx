import { Bookmark } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
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
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
        <SectionTitle titleId="practice-catalog-title" eyebrow="PRACTICE LIBRARY" title="Luyện tập theo 4 kỹ năng" description="Chọn một kỹ năng để bắt đầu hoặc tiếp tục lượt luyện tập đã lưu." />
        <Link to="/practice/saved" className="button button-secondary button-sm" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', marginTop: '1rem' }}>
          <Bookmark size={15} /> <span>Bài đã lưu</span>
        </Link>
      </div>
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
