import { useState } from 'react'
import Button from '../common/Button'
import GlassCard from '../common/GlassCard'

function RagUploadForm({ onUpload }) {
  const [form, setForm] = useState({ title: '', language: 'en', skill: 'GENERAL', rightsNote: '' })
  const [file, setFile] = useState(null)

  function update(field, value) { setForm((current) => ({ ...current, [field]: value })) }
  async function submit(event) {
    event.preventDefault()
    if (!file) return
    await onUpload(form, file)
    setForm({ title: '', language: 'en', skill: 'GENERAL', rightsNote: '' })
    setFile(null)
    event.target.reset()
  }

  return (
    <GlassCard className="admin-upload-card">
      <div className="admin-card-heading"><p className="eyebrow">NEW SOURCE</p><h2 className="font-display">Thêm học liệu</h2></div>
      <form className="admin-upload-form" onSubmit={submit}>
        <label>Tiêu đề<input required value={form.title} onChange={(event) => update('title', event.target.value)} /></label>
        <label>Ngôn ngữ<input required value={form.language} onChange={(event) => update('language', event.target.value)} /></label>
        <label>Kỹ năng<select value={form.skill} onChange={(event) => update('skill', event.target.value)}>{['GENERAL', 'READING', 'LISTENING', 'WRITING', 'SPEAKING'].map((skill) => <option key={skill}>{skill}</option>)}</select></label>
        <label>Ghi chú quyền sử dụng<textarea required value={form.rightsNote} onChange={(event) => update('rightsNote', event.target.value)} /></label>
        <label>File<input required type="file" accept=".pdf,.docx,.txt" onChange={(event) => setFile(event.target.files?.[0] ?? null)} /></label>
        <Button type="submit" disabled={!file}>Tải lên chờ duyệt</Button>
      </form>
    </GlassCard>
  )
}

export default RagUploadForm
