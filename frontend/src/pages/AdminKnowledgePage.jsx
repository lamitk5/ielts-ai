import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import PageContainer from '../components/common/PageContainer'
import { AdminDataState } from '../components/admin/AdminDataState'
import { adminPortalApi } from '../services/adminPortalApi'

export default function AdminKnowledgePage() {
  const [state, setState] = useState({ loading: true, error: '', items: [] })
  const [draft, setDraft] = useState({ promptKey: '', name: '', purpose: '', content: '' })
  const [saving, setSaving] = useState(false)
  const [notice, setNotice] = useState('')
  const load = () => adminPortalApi.prompts().then((items) => setState({ loading: false, error: '', items })).catch((error) => setState({ loading: false, error: error.message, items: [] }))
  useEffect(load, [])
  async function createDraft(event) {
    event.preventDefault(); setSaving(true); setNotice('')
    try { await adminPortalApi.createDraft(draft); setDraft({ promptKey: '', name: '', purpose: '', content: '' }); setNotice('Đã tạo bản nháp prompt.'); load() }
    catch (error) { setNotice(error.message) } finally { setSaving(false) }
  }
  return <PageContainer className="admin-page-content">
    <div className="admin-heading"><p className="eyebrow">KNOWLEDGE & AI</p><h1 className="font-display">Knowledge & Prompts</h1><p>RAG giữ quyền sử dụng nguồn; prompt có version và chỉ admin được xem nội dung.</p></div>
    <div className="admin-knowledge-actions"><Link className="button button-secondary" to="/admin/knowledge/rag">Mở Knowledge Base</Link></div>
    <form className="admin-prompt-form" onSubmit={createDraft} aria-label="Tạo prompt bản nháp">
      <h2>Tạo prompt bản nháp</h2>
      <div className="admin-prompt-grid"><label>Prompt key<input required value={draft.promptKey} onChange={(event) => setDraft({ ...draft, promptKey: event.target.value })} /></label><label>Tên hiển thị<input required value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} /></label><label>Mục đích<input value={draft.purpose} onChange={(event) => setDraft({ ...draft, purpose: event.target.value })} /></label><label>Nội dung<textarea required rows="4" value={draft.content} onChange={(event) => setDraft({ ...draft, content: event.target.value })} /></label></div>
      <button className="button button-primary" type="submit" disabled={saving}>{saving ? 'Đang lưu…' : 'Tạo bản nháp'}</button>{notice ? <p role="status">{notice}</p> : null}
    </form>
    <AdminDataState loading={state.loading} error={state.error} empty={!state.items.length}><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Prompt key</th><th>Tên</th><th>Version</th><th>Trạng thái</th></tr></thead><tbody>{state.items.map((item) => <tr key={item.versionId ?? item.promptKey}><td>{item.promptKey}</td><td>{item.name}</td><td>{item.versionNumber ?? '—'}</td><td>{item.status ?? 'Chưa có version'}</td></tr>)}</tbody></table></div></AdminDataState>
  </PageContainer>
}
