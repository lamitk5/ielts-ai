import { useEffect, useState } from 'react'
import { Pencil, Plus, Trash2, UploadCloud } from 'lucide-react'
import GlassCard from '../components/common/GlassCard'
import Button from '../components/common/Button'
import {
  createAdminMockTest,
  deleteAdminMockTest,
  listAdminMockTests,
  publishAdminMockTest,
  updateAdminMockTest,
} from '../services/mockTestApi'

const SKILLS = ['LISTENING', 'READING', 'WRITING', 'SPEAKING']
const defaultSections = () => [
  { order: 0, skill: 'LISTENING', practiceSetId: 'default-listening-set', timeLimitSeconds: 1800 },
  { order: 1, skill: 'READING', practiceSetId: 'default-reading-set', timeLimitSeconds: 3600 },
  { order: 2, skill: 'WRITING', practiceSetId: 'default-writing-set', timeLimitSeconds: 3600 },
  { order: 3, skill: 'SPEAKING', practiceSetId: 'default-speaking-set', timeLimitSeconds: 1800 },
]
const emptyForm = () => ({ slug: '', title: '', version: 'v1', published: false, sections: defaultSections() })

export default function AdminMockTestsPage() {
  const [tests, setTests] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [editingId, setEditingId] = useState(null)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const load = () => listAdminMockTests().then(setTests).catch((cause) => setError(cause.message))
  useEffect(() => { load() }, [])

  function updateSection(index, field, value) {
    setForm((current) => ({
      ...current,
      sections: current.sections.map((section, sectionIndex) => sectionIndex === index
        ? { ...section, [field]: field === 'timeLimitSeconds' ? Number(value) : value }
        : section),
    }))
  }

  function edit(test) {
    setEditingId(test.id)
    setForm({ slug: test.slug, title: test.title, version: test.version, published: test.published, sections: test.sections })
    setError('')
  }

  function reset() {
    setEditingId(null)
    setForm(emptyForm())
  }

  async function save(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      if (editingId) await updateAdminMockTest(editingId, form)
      else await createAdminMockTest(form)
      reset()
      await load()
    } catch (cause) {
      setError(cause.message)
    } finally {
      setSaving(false)
    }
  }

  async function publish(test) {
    try {
      const updated = await publishAdminMockTest(test.id, !test.published)
      setTests((current) => current.map((item) => item.id === updated.id ? updated : item))
    } catch (cause) {
      setError(cause.message)
    }
  }

  async function remove(test) {
    if (!window.confirm(`Xóa bản ${test.title}?`)) return
    try {
      await deleteAdminMockTest(test.id)
      setTests((current) => current.filter((item) => item.id !== test.id))
      if (editingId === test.id) reset()
    } catch (cause) {
      setError(cause.message)
    }
  }

  return <section className="admin-page" aria-labelledby="admin-mock-title">
    <div className="admin-page-heading"><p className="eyebrow">MOCK TEST MANAGEMENT</p><h1 id="admin-mock-title">Quản lý bài thi thử</h1><p>Tạo bản nháp, gán bộ bài theo từng kỹ năng và xuất bản cho học viên.</p></div>
    {error ? <div className="inline-error" role="alert">{error}</div> : null}
    <GlassCard className="admin-form-card"><form onSubmit={save} className="vocabulary-form">
      <h2>{editingId ? <Pencil size={18} /> : <Plus size={18} />} {editingId ? 'Chỉnh sửa bài thi' : 'Bài thi mới'}</h2>
      <label>Slug<input required value={form.slug} onChange={(event) => setForm({ ...form, slug: event.target.value })} /></label>
      <label>Tiêu đề<input required value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /></label>
      <label>Phiên bản<input value={form.version} onChange={(event) => setForm({ ...form, version: event.target.value })} /></label>
      <div className="mock-section-editor"><h3>Cấu hình phần thi</h3>{form.sections.map((section, index) => <div className="mock-section-row" key={`${section.order}-${index}`}>
        <span className="status-pill">{index + 1}</span>
        <label>Kỹ năng<select value={section.skill} onChange={(event) => updateSection(index, 'skill', event.target.value)}>{SKILLS.map((skill) => <option key={skill}>{skill}</option>)}</select></label>
        <label>Practice set<input required value={section.practiceSetId} onChange={(event) => updateSection(index, 'practiceSetId', event.target.value)} /></label>
        <label>Phút<input required min="1" type="number" value={Math.round(section.timeLimitSeconds / 60)} onChange={(event) => updateSection(index, 'timeLimitSeconds', Number(event.target.value) * 60)} /></label>
      </div>)}</div>
      <div className="admin-form-actions"><Button type="submit" disabled={saving}><UploadCloud size={15} /> {saving ? 'Đang lưu…' : editingId ? 'Lưu thay đổi' : 'Lưu bản nháp'}</Button>{editingId ? <Button type="button" variant="secondary" onClick={reset}>Hủy chỉnh sửa</Button> : null}</div>
    </form></GlassCard>
    <div className="admin-table-list">{tests.map((test) => <GlassCard key={test.id}><div className="admin-row"><div><h2>{test.title}</h2><p>{test.slug} · {test.sections.length} phần · {test.published ? 'Đã xuất bản' : 'Bản nháp'}</p></div><div className="admin-row-actions"><Button variant="secondary" onClick={() => edit(test)}><Pencil size={15} /> Sửa</Button><Button variant={test.published ? 'secondary' : 'primary'} onClick={() => publish(test)}>{test.published ? 'Gỡ xuất bản' : 'Xuất bản'}</Button><Button variant="secondary" onClick={() => remove(test)} aria-label={`Xóa ${test.title}`}><Trash2 size={15} /></Button></div></div></GlassCard>)}</div>
  </section>
}
