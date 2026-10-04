import { Bookmark, BookmarkCheck } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import GlassCard from '../components/common/GlassCard'
import { checkSavedStatus, savePractice, unsavePractice } from '../services/savedPracticeApi'
import { searchPractice } from '../services/searchApi'

const fallbackResults = [
  {
    id: 'task-1-academic-01',
    title: 'Academic Writing Task 1',
    skill: 'Writing',
    description: 'Write a concise summary of a visual prompt.',
    route: '/practice/writing',
    resultType: 'WRITING_PROMPT',
    typeLabel: 'Writing Prompt',
  },
]

const TYPE_FILTERS = [
  { id: 'ALL', label: 'Tất cả' },
  { id: 'PRACTICE_SET', label: 'Bài luyện' },
  { id: 'WRITING_PROMPT', label: 'Writing Prompt' },
  { id: 'SPEAKING_TOPIC', label: 'Speaking Topic' },
  { id: 'SUBMISSION_HISTORY', label: 'Lịch sử bài làm' },
]

function SearchPage() {
  const [searchParams] = useSearchParams()
  const query = searchParams.get('q')?.trim() ?? ''
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(Boolean(query))
  const [selectedType, setSelectedType] = useState('ALL')
  const [savedMap, setSavedMap] = useState({})

  useEffect(() => {
    let active = true
    if (!query) {
      setResults([])
      setLoading(false)
      return () => { active = false }
    }

    setLoading(true)
    searchPractice(query)
      .then((items) => {
        if (!active) return
        setResults(items)
        // Check saved status for practice sets
        items.filter((item) => item.resultType === 'PRACTICE_SET').forEach((item) => {
          checkSavedStatus(item.id)
            .then((res) => {
              if (active) {
                setSavedMap((prev) => ({ ...prev, [item.id]: res.saved }))
              }
            })
            .catch(() => {})
        })
      })
      .catch(() => {
        if (!active) return
        setResults(query.toLowerCase().includes('writing') ? fallbackResults : [])
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => { active = false }
  }, [query])

  async function handleToggleSave(event, item) {
    event.preventDefault()
    event.stopPropagation()
    const isCurrentlySaved = Boolean(savedMap[item.id])
    try {
      if (isCurrentlySaved) {
        await unsavePractice(item.id)
        setSavedMap((prev) => ({ ...prev, [item.id]: false }))
      } else {
        await savePractice(item.id)
        setSavedMap((prev) => ({ ...prev, [item.id]: true }))
      }
    } catch {
      // ignore or handle
    }
  }

  const filteredResults = selectedType === 'ALL'
    ? results
    : results.filter((r) => r.resultType === selectedType)

  return (
    <section className="search-page" aria-labelledby="search-title">
      <div className="search-page-header">
        <p className="eyebrow">KHÁM PHÁ BÀI LUYỆN</p>
        <h1 id="search-title" className="font-display">Tìm bài luyện tập</h1>
        <p className="foundation-copy">
          {query ? `Kết quả cho “${query}”` : 'Chọn một từ khóa để tìm nội dung luyện tập theo mục tiêu của bạn.'}
        </p>

        {results.length > 0 && (
          <div className="search-filter-pills" role="group" aria-label="Lọc theo loại kết quả" style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem', flexWrap: 'wrap' }}>
            {TYPE_FILTERS.map((filter) => (
              <button
                key={filter.id}
                type="button"
                className={`hero-suggestion-chip ${selectedType === filter.id ? 'active' : ''}`}
                style={{
                  background: selectedType === filter.id ? 'rgba(217, 119, 6, 0.25)' : undefined,
                  borderColor: selectedType === filter.id ? 'rgba(217, 119, 6, 0.6)' : undefined,
                }}
                onClick={() => setSelectedType(filter.id)}
              >
                {filter.label}
              </button>
            ))}
          </div>
        )}
      </div>

      {loading ? <p className="search-status" role="status">Đang tìm bài luyện tập…</p> : null}

      {!loading && filteredResults.length > 0 ? (
        <div className="search-results" aria-label="Kết quả tìm kiếm">
          {filteredResults.map((result) => {
            const isSaved = Boolean(savedMap[result.id])
            return (
              <GlassCard className="search-result-card" key={result.id} interactive>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.25rem' }}>
                  <p className="search-result-skill">{result.skill}</p>
                  <span
                    className="search-result-badge"
                    style={{
                      fontSize: '0.75rem',
                      padding: '0.15rem 0.5rem',
                      borderRadius: '9999px',
                      background: 'rgba(255, 255, 255, 0.08)',
                      border: '1px solid rgba(255, 255, 255, 0.12)',
                    }}
                  >
                    {result.typeLabel || result.resultType}
                  </span>
                </div>
                <h2><Link to={result.route}>{result.title}</Link></h2>
                <p>{result.description}</p>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem' }}>
                  <Link className="search-result-action" to={result.route}>
                    Bắt đầu luyện <span aria-hidden="true">→</span>
                  </Link>
                  {result.resultType === 'PRACTICE_SET' && (
                    <button
                      type="button"
                      aria-label={isSaved ? 'Bỏ lưu bài này' : 'Lưu bài này'}
                      onClick={(e) => handleToggleSave(e, result)}
                      style={{
                        background: 'transparent',
                        border: 'none',
                        color: isSaved ? '#d97706' : 'currentColor',
                        cursor: 'pointer',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.25rem',
                        fontSize: '0.85rem',
                      }}
                    >
                      {isSaved ? <BookmarkCheck size={18} /> : <Bookmark size={18} />}
                      <span>{isSaved ? 'Đã lưu' : 'Lưu bài'}</span>
                    </button>
                  )}
                </div>
              </GlassCard>
            )
          })}
        </div>
      ) : null}

      {!loading && !filteredResults.length ? (
        <GlassCard className="search-empty" role="status">
          Chưa tìm thấy bài luyện phù hợp. Hãy thử từ khóa khác.
        </GlassCard>
      ) : null}
    </section>
  )
}

export default SearchPage
