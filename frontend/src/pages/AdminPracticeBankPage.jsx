import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import PageContainer from '../components/common/PageContainer'
import { AdminDataState } from '../components/admin/AdminDataState'
import { adminPortalApi } from '../services/adminPortalApi'

export default function AdminPracticeBankPage() {
  const [state, setState] = useState({ loading: true, error: '', items: [] }); const [filter, setFilter] = useState('')
  const load = () => { setState((s) => ({ ...s, loading: true })); adminPortalApi.practices({ state: filter }).then((items) => setState({ loading: false, error: '', items })).catch((error) => setState({ loading: false, error: error.message, items: [] })) }
  useEffect(load, [filter])
  return <PageContainer className="admin-page-content"><div className="admin-heading"><p className="eyebrow">AI & CONTENT</p><h1 className="font-display">Practice Bank</h1><p>Phiên bản đã duyệt là bất biến; learner chỉ nhìn thấy nội dung được publish.</p></div><label className="admin-filter">Trạng thái<select value={filter} onChange={(event) => setFilter(event.target.value)}><option value="">Tất cả</option><option value="PENDING_REVIEW">Chờ duyệt</option><option value="APPROVED">Đã duyệt</option><option value="NEEDS_REVISION">Cần chỉnh sửa</option><option value="REJECTED">Từ chối</option></select></label><AdminDataState loading={state.loading} error={state.error} empty={!state.items.length}><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Tiêu đề</th><th>Kỹ năng</th><th>Trạng thái</th><th>Hành động</th></tr></thead><tbody>{state.items.map((item) => <tr key={item.id}><td>{item.title}</td><td>{item.skill}</td><td><span className="admin-status-pill">{item.state}</span></td><td><Link to={`/admin/generator/sets/${item.id}`}>Mở review</Link></td></tr>)}</tbody></table></div></AdminDataState></PageContainer>
}
