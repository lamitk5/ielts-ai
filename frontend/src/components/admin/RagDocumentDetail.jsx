import GlassCard from '../common/GlassCard'

const RIGHTS_LABELS = { PENDING_REVIEW: 'Chờ duyệt', APPROVED: 'Đã duyệt', RESTRICTED: 'Hạn chế', REJECTED: 'Từ chối' }
const INDEX_LABELS = { PENDING: 'Chờ xử lý', INDEXED: 'Đã lập chỉ mục', ACTIVE: 'Đang sử dụng', FAILED: 'Lỗi chỉ mục' }

function RagDocumentDetail({ detail }) {
  if (!detail) return <GlassCard className="admin-detail-card"><p className="admin-muted">Chọn một nguồn để xem preview.</p></GlassCard>
  return <GlassCard className="admin-detail-card"><p className="eyebrow">CHI TIẾT</p><h2 className="font-display">{detail.title}</h2><div className="admin-detail-status"><span>{RIGHTS_LABELS[detail.rightsStatus] || detail.rightsStatus}</span><span>{INDEX_LABELS[detail.indexStatus] || detail.indexStatus}</span></div><h3>Xem trước giới hạn</h3>{detail.preview ? <p className="admin-preview">{detail.preview.text}</p> : <p className="admin-muted">Chưa có bản xem trước.</p>}</GlassCard>
}

export default RagDocumentDetail
