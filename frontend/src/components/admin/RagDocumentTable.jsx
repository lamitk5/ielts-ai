import Button from '../common/Button'
import GlassCard from '../common/GlassCard'

const RIGHTS_LABELS = { PENDING_REVIEW: 'Chờ duyệt', APPROVED: 'Đã duyệt', RESTRICTED: 'Hạn chế', REJECTED: 'Từ chối' }
const INDEX_LABELS = { PENDING: 'Chờ xử lý', INDEXED: 'Đã lập chỉ mục', ACTIVE: 'Đang sử dụng', FAILED: 'Lỗi chỉ mục' }
const SKILL_LABELS = { GENERAL: 'Tổng quát', READING: 'Reading', LISTENING: 'Listening', WRITING: 'Writing', SPEAKING: 'Speaking' }

function RagDocumentTable({ documents, onSelect, onAction }) {
  return (
    <GlassCard className="admin-table-card">
      <div className="admin-card-heading"><p className="eyebrow">KHO TRI THỨC</p><h2 className="font-display">Nguồn học liệu</h2></div>
      <div className="admin-table-scroll"><table><caption className="sr-only">Danh sách nguồn học liệu</caption><thead><tr><th>Tiêu đề</th><th>Quyền sử dụng</th><th>Trạng thái chỉ mục</th><th>Thao tác</th></tr></thead><tbody>
        {documents.map((document) => <tr key={document.id} aria-label={document.title}>
          <th scope="row">{document.title}<small>{SKILL_LABELS[document.skill] || document.skill}</small></th>
          <td><span className="admin-status">{RIGHTS_LABELS[document.rightsStatus] || document.rightsStatus}</span></td><td><span className="admin-status">{INDEX_LABELS[document.indexStatus] || document.indexStatus}</span></td>
          <td className="admin-actions"><Button size="sm" variant="ghost" onClick={() => onSelect(document.id)}>Xem</Button><Button size="sm" variant="secondary" onClick={() => onAction(document, 'approve')} disabled={document.rightsStatus !== 'PENDING_REVIEW'}>Duyệt</Button><Button size="sm" variant="secondary" onClick={() => onAction(document, 'index')} disabled={document.rightsStatus !== 'APPROVED' || document.indexStatus === 'INDEXED'}>Lập chỉ mục</Button><Button size="sm" variant="secondary" onClick={() => onAction(document, 'activate')} disabled={document.rightsStatus !== 'APPROVED' || document.indexStatus !== 'INDEXED' || document.active}>Kích hoạt</Button></td>
        </tr>)}
      </tbody></table></div>
    </GlassCard>
  )
}

export default RagDocumentTable
