import { useEffect, useState } from 'react'
import PageContainer from '../components/common/PageContainer'
import { AdminDataState } from '../components/admin/AdminDataState'
import { adminPortalApi } from '../services/adminPortalApi'

const ROLE_LABELS = { ADMIN: 'Quản trị viên', CUSTOMER: 'Học viên', USER: 'Học viên' }

export default function AdminLearnersPage() { const [state, setState] = useState({ loading: true, error: '', data: null }); useEffect(() => { adminPortalApi.learners({ size: 50 }).then((data) => setState({ loading: false, error: '', data })).catch((error) => setState({ loading: false, error: error.message, data: null })) }, []); return <PageContainer className="admin-page-content"><div className="admin-heading"><p className="eyebrow">QUẢN LÝ</p><h1 className="font-display">Quản lý học viên</h1><p>Danh sách học viên để hỗ trợ vận hành, không hiển thị mật khẩu hay dữ liệu nhạy cảm.</p></div><AdminDataState loading={state.loading} error={state.error} empty={!state.data?.items?.length}><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Email</th><th>Tên</th><th>Vai trò</th><th>Tham gia</th></tr></thead><tbody>{state.data?.items.map((item) => <tr key={item.id}><td>{item.email}</td><td>{item.firstName}</td><td>{ROLE_LABELS[item.role] || item.role}</td><td>{new Date(item.createdAt).toLocaleDateString('vi-VN')}</td></tr>)}</tbody></table></div></AdminDataState></PageContainer> }
