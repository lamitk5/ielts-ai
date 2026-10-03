import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { BarChart3, BookOpenCheck, ClipboardCheck, FileText, History, LayoutDashboard, Menu, ShieldCheck, Users, X } from 'lucide-react'

const groups = [
  { label: 'TỔNG QUAN', items: [['Dashboard', '/admin', LayoutDashboard]] },
  { label: 'AI & CONTENT', items: [['AI Generator', '/admin/generator', FileText], ['Practice Bank', '/admin/practices', BookOpenCheck]] },
  { label: 'KNOWLEDGE & AI', items: [['Knowledge & Prompts', '/admin/knowledge', ShieldCheck]] },
  { label: 'ASSESSMENT', items: [['Review Queue', '/admin/reviews', ClipboardCheck]] },
  { label: 'MANAGEMENT', items: [['Learners', '/admin/learners', Users], ['API Usage & Cost', '/admin/usage', BarChart3]] },
  { label: 'SYSTEM', items: [['Audit Log', '/admin/audit', History]] },
]

export default function AdminLayout() {
  const [open, setOpen] = useState(false)
  return <div className="admin-shell">
    <button className="admin-mobile-toggle" type="button" aria-label={open ? 'Đóng menu quản trị' : 'Mở menu quản trị'} onClick={() => setOpen((value) => !value)}>{open ? <X /> : <Menu />}</button>
    <aside className={`admin-sidebar ${open ? 'is-open' : ''}`}>
      <Link className="admin-brand" to="/admin"><span className="eyebrow">LUMEN IELTS</span><strong>Admin Studio</strong></Link>
      <nav aria-label="Điều hướng quản trị" onClick={() => setOpen(false)}>
        {groups.map((group) => <div className="admin-nav-group" key={group.label}><span className="admin-nav-label">{group.label}</span>{group.items.map(([label, href, Icon]) => <NavLink end={href === '/admin'} className="admin-nav-link" to={href} key={href}><Icon aria-hidden="true" />{label}</NavLink>)}</div>)}
      </nav>
      <Link className="admin-back-link" to="/">← Về giao diện học viên</Link>
    </aside>
    <main className="admin-main"><header className="admin-topbar"><span>Không gian quản trị</span><span className="admin-status"><i /> Bảo mật server-side</span></header><Outlet /></main>
  </div>
}
