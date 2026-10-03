import { useEffect, useState } from 'react'
import PageContainer from '../components/common/PageContainer'
import { AdminDataState } from '../components/admin/AdminDataState'
import { adminPortalApi } from '../services/adminPortalApi'

export default function AdminReviewQueuePage() {
  const [state, setState] = useState({ loading: true, error: '', items: [] })
  const load = () => adminPortalApi.reviews().then((items) => setState({ loading: false, error: '', items })).catch((error) => setState({ loading: false, error: error.message, items: [] }))
  useEffect(load, [])
  async function resolve(id) { await adminPortalApi.resolveReport(id); load() }
  return <PageContainer className="admin-page-content"><div className="admin-heading"><p className="eyebrow">ASSESSMENT</p><h1 className="font-display">Review Queue</h1><p>Ưu tiên báo cáo và tách biệt kết quả AI với nhận xét người chấm.</p></div><AdminDataState loading={state.loading} error={state.error} empty={!state.items.length}><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Học viên</th><th>Kỹ năng</th><th>Loại</th><th>Trạng thái</th><th /></tr></thead><tbody>{state.items.map((item) => <tr key={item.id}><td>{item.ownerEmail}</td><td>{item.skill}</td><td>{item.category}</td><td>{item.status}</td><td>{item.status === 'OPEN' && <button className="button button-ghost" onClick={() => resolve(item.id)}>Đánh dấu đã xử lý</button>}</td></tr>)}</tbody></table></div></AdminDataState></PageContainer>
}
