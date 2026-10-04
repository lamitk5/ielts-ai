import { BookOpen, Check, Eye, Plus, RotateCcw, Search, Trash2 } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import GlassCard from '../components/common/GlassCard'
import Button from '../components/common/Button'
import { createVocabularyItem, deleteVocabularyItem, listVocabulary, reviewVocabularyItem } from '../services/vocabularyApi'

const emptyForm = { word: '', meaning: '', exampleSentence: '', note: '', source: 'manual' }

export default function VocabularyPage() {
  const [items, setItems] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [revealed, setRevealed] = useState(null)

  const load = () => {
    setLoading(true); setError('')
    listVocabulary({ query, status }).then(setItems).catch((cause) => setError(cause.message)).finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [query, status]) // eslint-disable-line react-hooks/exhaustive-deps

  const counts = useMemo(() => ({ total: items.length, new: items.filter((item) => item.status === 'NEW').length, learning: items.filter((item) => item.status === 'LEARNING').length, mastered: items.filter((item) => item.status === 'MASTERED').length }), [items])
  const updateField = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }))

  async function add(event) {
    event.preventDefault(); if (!form.word.trim() || !form.meaning.trim()) return
    setBusy(true); setError('')
    try { const item = await createVocabularyItem({ ...form, word: form.word.trim(), meaning: form.meaning.trim() }); setItems((current) => [item, ...current]); setForm(emptyForm) }
    catch (cause) { setError(cause.message) } finally { setBusy(false) }
  }
  async function review(item, nextStatus) { try { const updated = await reviewVocabularyItem(item.id, nextStatus); setItems((current) => current.map((entry) => entry.id === item.id ? updated : entry)) } catch (cause) { setError(cause.message) } }
  async function remove(item) { try { await deleteVocabularyItem(item.id); setItems((current) => current.filter((entry) => entry.id !== item.id)) } catch (cause) { setError(cause.message) } }

  return <section className="learning-page vocabulary-page" aria-labelledby="vocabulary-title">
    <div className="search-page-header">
      <p className="eyebrow">VOCABULARY NOTEBOOK</p><h1 id="vocabulary-title" className="font-display">Sổ tay từ vựng</h1>
      <p className="foundation-copy">Lưu lại những từ quan trọng, ôn lại theo nhịp học của bạn và giữ quyền sở hữu dữ liệu cá nhân.</p>
    </div>
    {error ? <div className="inline-error" role="alert">{error}</div> : null}
    <div className="learning-stat-grid" aria-label="Thống kê từ vựng">
      <span><strong>{counts.total}</strong>Tổng số</span><span><strong>{counts.new}</strong>Mới</span><span><strong>{counts.learning}</strong>Đang học</span><span><strong>{counts.mastered}</strong>Đã thuộc</span>
    </div>
    <GlassCard className="vocabulary-add-card"><form onSubmit={add} className="vocabulary-form">
      <h2><Plus size={18} /> Thêm từ mới</h2>
      <label>Từ mới<input aria-label="Từ mới" value={form.word} onChange={updateField('word')} required /></label>
      <label>Nghĩa<input aria-label="Nghĩa" value={form.meaning} onChange={updateField('meaning')} required /></label>
      <label>Câu ví dụ<input value={form.exampleSentence} onChange={updateField('exampleSentence')} /></label>
      <label>Ghi chú<input value={form.note} onChange={updateField('note')} /></label>
      <Button type="submit" disabled={busy}>{busy ? 'Đang lưu…' : 'Thêm từ'}</Button>
    </form></GlassCard>
    <div className="vocabulary-toolbar"><label className="search-input"><Search size={16} /><span className="sr-only">Tìm từ</span><input placeholder="Tìm trong sổ tay…" value={query} onChange={(event) => setQuery(event.target.value)} /></label><select aria-label="Lọc trạng thái" value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Tất cả trạng thái</option><option value="NEW">Mới</option><option value="LEARNING">Đang học</option><option value="MASTERED">Đã thuộc</option></select></div>
    {loading ? <p role="status" className="search-status">Đang tải sổ tay…</p> : items.length === 0 ? <GlassCard className="learning-empty-state"><BookOpen size={28} /><p>Chưa có từ vựng phù hợp.</p><Link to="/practice">Tìm bài luyện để bắt đầu</Link></GlassCard> : <div className="vocabulary-grid">{items.map((item) => <GlassCard key={item.id} className="vocabulary-card">
      <div className="vocabulary-card-head"><span className="vocabulary-word">{item.word}</span><button type="button" className="icon-button" aria-label={`Xóa ${item.word}`} onClick={() => remove(item)}><Trash2 size={16} /></button></div>
      <p className="vocabulary-meaning">{revealed === item.id ? item.meaning : '••••••••'}</p>{item.exampleSentence ? <p className="vocabulary-example">{item.exampleSentence}</p> : null}
      <div className="vocabulary-card-actions"><Button variant="ghost" size="sm" onClick={() => setRevealed((value) => value === item.id ? null : item.id)}><Eye size={14} /> {revealed === item.id ? 'Ẩn nghĩa' : 'Xem nghĩa'}</Button><button type="button" className="text-action" onClick={() => review(item, 'LEARNING')}><RotateCcw size={14} /> Ôn lại</button><button type="button" className="text-action" onClick={() => review(item, 'MASTERED')}><Check size={14} /> Đã thuộc</button></div>
      <span className={`status-pill status-${item.status.toLowerCase()}`}>{item.status === 'NEW' ? 'Mới' : item.status === 'LEARNING' ? 'Đang học' : 'Đã thuộc'}</span>
    </GlassCard>)}</div>}
  </section>
}
