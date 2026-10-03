import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import PageContainer from '../components/common/PageContainer'
import GlassCard from '../components/common/GlassCard'
import { AdminDataState } from '../components/admin/AdminDataState'
import { adminPortalApi } from '../services/adminPortalApi'

export default function AdminDashboardPage() {
  const [state, setState] = useState({ loading: true, error: '', data: null })
  useEffect(() => { adminPortalApi.overview().then((data) => setState({ loading: false, error: '', data })).catch((error) => setState({ loading: false, error: error.message, data: null })) }, [])
  const data = state.data
  return <PageContainer className="admin-page-content"><div className="admin-heading"><p className="eyebrow">ADMINISTRATION</p><h1 className="font-display">Dashboard vận hành</h1><p>Số liệu được lấy từ dữ liệu thật; không tạo số liệu mẫu cho màn hình quản trị.</p></div><AdminDataState loading={state.loading} error={state.error} empty={!data}><div className="admin-kpi-grid">{[['Học viên', data?.learners], ['Bộ đề', data?.practiceSets], ['Chờ duyệt', data?.pendingReview], ['Báo cáo mở', data?.openReports], ['Prompt active', data?.activePrompts]].map(([label, value]) => <GlassCard className="admin-kpi" key={label}><span>{label}</span><strong>{value}</strong></GlassCard>)}</div><div className="admin-quick-grid"><GlassCard><h2>Cần xử lý</h2><p>{data?.pendingReview ? `${data.pendingReview} bộ đề chờ review.` : 'Không có bộ đề chờ review.'}</p><Link className="button button-secondary" to="/admin/reviews">Mở Review Queue</Link></GlassCard><GlassCard><h2>Không gian AI</h2><p>Quản lý nguồn RAG và prompt theo phiên bản.</p><Link className="button button-secondary" to="/admin/knowledge">Mở Knowledge & Prompts</Link></GlassCard></div></AdminDataState></PageContainer>
}
