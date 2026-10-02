import { useState } from 'react'
import GlassCard from '../common/GlassCard'

export default function SubmissionReviewPanel({ result, onSubmit, submitting = false }) {
  const [feedback, setFeedback] = useState('')
  const [band, setBand] = useState('')
  return <GlassCard className="submission-review-panel"><h2>{result?.skill || 'Submission'} review</h2><p>Phản hồi người chấm được lưu riêng, không ghi đè kết quả AI.</p><label>Band ước lượng<input value={band} onChange={(event) => setBand(event.target.value)} inputMode="decimal" /></label><label>Nhận xét<textarea value={feedback} onChange={(event) => setFeedback(event.target.value)} rows="5" /></label><button className="button button-primary" disabled={submitting} onClick={() => onSubmit({ overallBand: band ? Number(band) : null, reviewerFeedback: feedback })}>{submitting ? 'Đang lưu…' : 'Lưu nhận xét'}</button></GlassCard>
}
