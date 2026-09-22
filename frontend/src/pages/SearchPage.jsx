import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import GlassCard from '../components/common/GlassCard'
import { searchPractice } from '../services/searchApi'

const fallbackResults = [
  {
    id: 'task-1-academic-01',
    title: 'Academic Writing Task 1',
    skill: 'Writing',
    description: 'Write a concise summary of a visual prompt.',
    route: '/practice/writing',
  },
]

function SearchPage() {
  const [searchParams] = useSearchParams()
  const query = searchParams.get('q')?.trim() ?? ''
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(Boolean(query))

  useEffect(() => {
    let active = true
    if (!query) {
      setResults([])
      setLoading(false)
      return () => { active = false }
    }

    setLoading(true)
    searchPractice(query)
      .then((items) => active && setResults(items))
      .catch(() => active && setResults(query.toLowerCase().includes('writing') ? fallbackResults : []))
      .finally(() => active && setLoading(false))

    return () => { active = false }
  }, [query])

  return (
    <section className="search-page" aria-labelledby="search-title">
      <div className="search-page-header">
        <p className="eyebrow">KHÁM PHÁ BÀI LUYỆN</p>
        <h1 id="search-title" className="font-display">Tìm bài luyện tập</h1>
        <p className="foundation-copy">
          {query ? `Kết quả cho “${query}”` : 'Chọn một từ khóa để tìm nội dung luyện tập theo mục tiêu của bạn.'}
        </p>
      </div>
      {loading ? <p className="search-status" role="status">Đang tìm bài luyện tập…</p> : null}
      {!loading && results.length ? (
        <div className="search-results" aria-label="Kết quả tìm kiếm">
          {results.map((result) => (
            <GlassCard className="search-result-card" key={result.id} interactive>
              <p className="search-result-skill">{result.skill}</p>
              <h2><Link to={result.route}>{result.title}</Link></h2>
              <p>{result.description}</p>
              <Link className="search-result-action" to={result.route}>Bắt đầu luyện <span aria-hidden="true">→</span></Link>
            </GlassCard>
          ))}
        </div>
      ) : null}
      {!loading && !results.length ? <GlassCard className="search-empty" role="status">Chưa tìm thấy bài luyện phù hợp. Hãy thử từ khóa khác.</GlassCard> : null}
    </section>
  )
}

export default SearchPage
