import { useState } from 'react'
import Button from '../common/Button'
import GlassCard from '../common/GlassCard'
import { writeAdminToken } from '../../services/ragAdminApi'

function RagAdminUnlock({ onUnlock }) {
  const [token, setToken] = useState('')

  function submit(event) {
    event.preventDefault()
    const value = writeAdminToken(token)
    if (value) onUnlock(value)
  }

  return (
    <section className="admin-page admin-unlock" aria-labelledby="admin-unlock-title">
      <GlassCard className="admin-unlock-card">
        <p className="eyebrow">RAG ADMIN</p>
        <h1 id="admin-unlock-title" className="font-display">Mở khóa quản trị học liệu</h1>
        <p>Nhập token phiên local để quản lý nguồn học liệu và quyền sử dụng.</p>
        <form onSubmit={submit} className="admin-unlock-form">
          <label htmlFor="admin-token">Admin token</label>
          <input id="admin-token" type="password" value={token} onChange={(event) => setToken(event.target.value)} autoComplete="off" />
          <Button type="submit" disabled={!token.trim()}>Mở khóa</Button>
        </form>
      </GlassCard>
    </section>
  )
}

export default RagAdminUnlock
