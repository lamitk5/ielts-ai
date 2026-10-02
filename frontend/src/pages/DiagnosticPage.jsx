import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import DiagnosticStepper from '../components/diagnostic/DiagnosticStepper'
import DiagnosticResult from '../components/diagnostic/DiagnosticResult'
import { finishDiagnostic, getDiagnosticResult, getDiagnosticSession, markDiagnosticUnavailable } from '../services/diagnosticApi'

export default function DiagnosticPage() {
  const [session, setSession] = useState(null)
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const refresh = async (nextSession) => {
    const current = nextSession || await getDiagnosticSession()
    setSession(current)
    setResult(await getDiagnosticResult(current.id))
  }

  useEffect(() => { refresh().catch((cause) => setError(cause.message)) }, [])

  const markUnavailable = async (skill) => {
    setBusy(true); setError('')
    try { await markDiagnosticUnavailable(session.id, skill, 'Phần này hiện chưa có dữ liệu đo lường phù hợp.'); await refresh(session) }
    catch (cause) { setError(cause.message) } finally { setBusy(false) }
  }
  const finish = async () => {
    setBusy(true); setError('')
    try { const next = await finishDiagnostic(session.id, true); await refresh(next) }
    catch (cause) { setError(cause.message) } finally { setBusy(false) }
  }

  return (
    <main className="diagnostic-page">
      <div className="diagnostic-page-header">
        <p className="eyebrow">LỘ TRÌNH CÁ NHÂN</p>
        <h1 className="font-display">Đánh giá năng lực khởi điểm</h1>
        <p className="foundation-copy">Đây là hồ sơ khởi điểm ước lượng dựa trên dữ liệu luyện tập thật, không phải điểm thi hay xếp lớp IELTS chính thức.</p>
      </div>
      {error ? <div role="alert" className="inline-error">{error} <Link to="/login">Đăng nhập lại</Link></div> : null}
      {session ? <DiagnosticStepper session={session} sections={result?.sections} onUnavailable={markUnavailable} onFinish={finish} busy={busy} /> : null}
      <DiagnosticResult result={result} />
    </main>
  )
}
