import GlassCard from '../common/GlassCard'

function RagDocumentDetail({ detail }) {
  if (!detail) return <GlassCard className="admin-detail-card"><p className="admin-muted">Chọn một nguồn để xem preview.</p></GlassCard>
  return <GlassCard className="admin-detail-card"><p className="eyebrow">DETAIL</p><h2 className="font-display">{detail.title}</h2><div className="admin-detail-status"><span>{detail.rightsStatus}</span><span>{detail.indexStatus}</span></div><h3>Preview giới hạn</h3>{detail.preview ? <p className="admin-preview">{detail.preview.text}</p> : <p className="admin-muted">Chưa có preview.</p>}</GlassCard>
}

export default RagDocumentDetail
