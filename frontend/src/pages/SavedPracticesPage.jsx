import { BookmarkCheck, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../components/common/Button'
import GlassCard from '../components/common/GlassCard'
import { listSavedPractices, unsavePractice } from '../services/savedPracticeApi'

const SKILL_FILTERS = [
  { id: '', label: 'Tất cả' },
  { id: 'reading', label: 'Reading' },
  { id: 'listening', label: 'Listening' },
  { id: 'writing', label: 'Writing' },
  { id: 'speaking', label: 'Speaking' },
]

export default function SavedPracticesPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedSkill, setSelectedSkill] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    listSavedPractices({ skill: selectedSkill })
      .then((data) => {
        if (active) setItems(data)
      })
      .catch((err) => {
        if (active) setError(err.message || 'Không thể tải danh sách bài đã lưu.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => { active = false }
  }, [selectedSkill])

  async function handleUnsave(publishedSetId) {
    try {
      await unsavePractice(publishedSetId)
      setItems((prev) => prev.filter((item) => item.publishedSetId !== publishedSetId))
    } catch {
      // optimistic update rollback if needed
    }
  }

  return (
    <section className="saved-practices-page" aria-labelledby="saved-practices-title" style={{ padding: '2rem 1rem', maxWidth: '1200px', margin: '0 auto' }}>
      <div className="search-page-header" style={{ marginBottom: '2rem' }}>
        <p className="eyebrow">BỘ SƯU TẬP CỦA BẠN</p>
        <h1 id="saved-practices-title" className="font-display" style={{ fontSize: '2.25rem', marginBottom: '0.5rem' }}>
          Bài đã lưu
        </h1>
        <p className="foundation-copy">
          Danh sách các bài luyện tập IELTS bạn đã lưu để ôn tập và luyện lại.
        </p>

        <div className="skill-filter-pills" role="group" aria-label="Lọc theo kỹ năng" style={{ display: 'flex', gap: '0.5rem', marginTop: '1.25rem', flexWrap: 'wrap' }}>
          {SKILL_FILTERS.map((filter) => (
            <button
              key={filter.id}
              type="button"
              className={`hero-suggestion-chip ${selectedSkill === filter.id ? 'active' : ''}`}
              style={{
                background: selectedSkill === filter.id ? 'rgba(217, 119, 6, 0.25)' : undefined,
                borderColor: selectedSkill === filter.id ? 'rgba(217, 119, 6, 0.6)' : undefined,
              }}
              onClick={() => setSelectedSkill(filter.id)}
            >
              {filter.label}
            </button>
          ))}
        </div>
      </div>

      {loading && <p className="search-status" role="status">Đang tải danh sách bài đã lưu…</p>}
      {error && <GlassCard className="search-empty" role="alert">{error}</GlassCard>}

      {!loading && !error && items.length > 0 && (
        <div className="saved-practices-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.25rem' }}>
          {items.map((item) => {
            const skill = (item.skill || 'general').toLowerCase()
            const route = `/practice/${skill}/${item.publishedSetId}`
            return (
              <GlassCard key={item.id || item.publishedSetId} className="saved-practice-card" interactive>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <span
                    className="skill-tag"
                    style={{
                      textTransform: 'uppercase',
                      fontSize: '0.75rem',
                      fontWeight: 600,
                      color: '#d97706',
                      letterSpacing: '0.05em',
                    }}
                  >
                    {item.skill}
                  </span>
                  <button
                    type="button"
                    aria-label={`Bỏ lưu ${item.title}`}
                    onClick={() => handleUnsave(item.publishedSetId)}
                    style={{
                      background: 'transparent',
                      border: 'none',
                      color: 'rgba(255, 255, 255, 0.5)',
                      cursor: 'pointer',
                      padding: '0.25rem',
                    }}
                    title="Bỏ lưu"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>

                <h2 style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>
                  <Link to={route} style={{ color: 'inherit', textDecoration: 'none' }}>
                    {item.title}
                  </Link>
                </h2>

                <p style={{ fontSize: '0.875rem', color: 'rgba(255, 255, 255, 0.7)', marginBottom: '1.25rem' }}>
                  Đã lưu vào {new Date(item.savedAt).toLocaleDateString('vi-VN')}
                </p>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Link className="search-result-action" to={route}>
                    Luyện tập ngay <span aria-hidden="true">→</span>
                  </Link>
                  {!item.available && (
                    <span style={{ fontSize: '0.75rem', color: '#ef4444' }}>
                      (Tạm thời không khả dụng)
                    </span>
                  )}
                </div>
              </GlassCard>
            )
          })}
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <GlassCard className="search-empty" role="status" style={{ textAlign: 'center', padding: '3rem 1.5rem' }}>
          <BookmarkCheck size={36} style={{ color: '#d97706', margin: '0 auto 1rem auto' }} />
          <h2 style={{ fontSize: '1.25rem', marginBottom: '0.5rem' }}>Chưa có bài luyện nào được lưu</h2>
          <p style={{ color: 'rgba(255, 255, 255, 0.7)', marginBottom: '1.5rem' }}>
            Hãy duyệt qua kho bài luyện tập hoặc tìm kiếm các chủ đề IELTS để lưu lại luyện sau.
          </p>
          <Link to="/practice">
            <Button variant="primary" size="md">Khám phá bài luyện</Button>
          </Link>
        </GlassCard>
      )}
    </section>
  )
}
