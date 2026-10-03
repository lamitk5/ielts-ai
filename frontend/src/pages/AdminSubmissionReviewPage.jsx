import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import PageContainer from '../components/common/PageContainer'
import GlassCard from '../components/common/GlassCard'
import SubmissionReviewPanel from '../components/admin/SubmissionReviewPanel'
import { getAdminSubmissionResult, getAdminSubmissions, submitSubmissionReview } from '../services/resultsApi'

export default function AdminSubmissionReviewPage() {
  const [queue, setQueue] = useState(null)
  const [selected, setSelected] = useState(null)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  useEffect(() => { getAdminSubmissions().then(setQueue).catch((e) => setError(e.message)) }, [])
  const select = (id) => getAdminSubmissionResult(id).then(setSelected).catch((e) => setError(e.message))
  const review = (payload) => { setSubmitting(true); return submitSubmissionReview(selected.submissionId, payload).then(() => select(selected.submissionId)).catch((e) => setError(e.message)).finally(() => setSubmitting(false)) }
  return <PageContainer className="py-8 space-y-6"><div><p className="eyebrow">QUẢN TRỊ</p><h1 className="font-display">Kiểm duyệt bài nộp</h1><p>AI và nhận xét người chấm được hiển thị thành hai nguồn độc lập.</p></div>{error && <p role="alert">{error}</p>}<div className="admin-submission-layout"><GlassCard><h2>Hàng đợi</h2>{(queue?.items ?? []).map((item) => <button key={item.id} className="button button-ghost w-full justify-between" onClick={() => select(item.id)}>{item.skill} · {item.status}</button>)}{queue && !queue.items?.length && <p>Chưa có bài cần xem.</p>}</GlassCard>{selected ? <SubmissionReviewPanel result={selected} onSubmit={review} submitting={submitting} /> : <GlassCard><p>Chọn một bài nộp để xem chi tiết.</p><Link to="/admin/generator">Mở khu vực tạo bài</Link></GlassCard>}</div></PageContainer>
}
