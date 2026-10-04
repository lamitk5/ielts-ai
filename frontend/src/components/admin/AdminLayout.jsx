import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { BarChart3, BookOpenCheck, ClipboardCheck, FileText, History, LayoutDashboard, Menu, ShieldCheck, Users, X, ClipboardList } from 'lucide-react'

const groups = [
  { label: 'TỔNG QUAN', items: [['Tổng quan', '/admin', LayoutDashboard]] },
  { label: 'AI & NỘI DUNG', items: [['Trình tạo bài AI', '/admin/generator', FileText], ['Ngân hàng bài luyện', '/admin/practices', BookOpenCheck], ['Bài thi thử', '/admin/mock-tests', ClipboardList]] },
  { label: 'TRI THỨC & AI', items: [['Kho tri thức & Prompt', '/admin/knowledge', ShieldCheck]] },
  { label: 'ĐÁNH GIÁ', items: [['Hàng đợi đánh giá', '/admin/reviews', ClipboardCheck]] },
  { label: 'QUẢN LÝ', items: [['Học viên', '/admin/learners', Users], ['Sử dụng API & Chi phí', '/admin/usage', BarChart3]] },
  { label: 'HỆ THỐNG', items: [['Nhật ký hoạt động', '/admin/audit', History]] },
]

export default function AdminLayout() {
  const [open, setOpen] = useState(false)
  return <div className="admin-shell">
    <button className="admin-mobile-toggle" type="button" aria-label={open ? 'Đóng menu quản trị' : 'Mở menu quản trị'} onClick={() => setOpen((value) => !value)}>{open ? <X /> : <Menu />}</button>
    <aside className={`admin-sidebar ${open ? 'is-open' : ''}`}>
      <Link className="admin-brand" to="/admin"><span className="eyebrow">LUMEN IELTS</span><strong>Không gian quản trị</strong></Link>
      <nav aria-label="Điều hướng quản trị" onClick={() => setOpen(false)}>
        {groups.map((group) => <div className="admin-nav-group" key={group.label}><span className="admin-nav-label">{group.label}</span>{group.items.map(([label, href, Icon]) => <NavLink end={href === '/admin'} className="admin-nav-link" to={href} key={href}><Icon aria-hidden="true" />{label}</NavLink>)}</div>)}
      </nav>
      <Link className="admin-back-link" to="/">← Về giao diện học viên</Link>
    </aside>
    <main className="admin-main"><header className="admin-topbar"><span>Không gian quản trị</span><span className="admin-status"><i /> Bảo mật phía máy chủ</span></header><Outlet /></main>
  </div>
}
