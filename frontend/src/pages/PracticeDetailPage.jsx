import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import GlassCard from '../components/common/GlassCard'
import PracticeErrorState from '../components/practice/PracticeErrorState'
import { getPracticeSet } from '../services/practiceCatalogApi'

function PracticeDetailPage() {
  const { skill, setId } = useParams()
  const [practiceSet, setPracticeSet] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => { getPracticeSet(skill, setId).then(setPracticeSet).catch((err) => setError(err.message)) }, [skill, setId])
  if (error) return <PracticeErrorState message={error} />
  if (!practiceSet) return <p role="status">Đang tải bài luyện tập…</p>
  return <section className="practice-detail-page" aria-labelledby="practice-detail-title">
    <p className="eyebrow">{String(skill).toUpperCase()}</p>
    <h1 id="practice-detail-title" className="font-display">{practiceSet.title ?? practiceSet.name}</h1>
    <p>{practiceSet.description}</p>
    <GlassCard><strong>{practiceSet.questions?.length ?? 0} câu hỏi</strong><p>Phiên bản hiện hành của bộ đề đã được công bố.</p><Link className="button button-primary" to={`/practice/${skill}/${setId}/attempt`}>Bắt đầu / tiếp tục</Link></GlassCard>
  </section>
}

export default PracticeDetailPage
