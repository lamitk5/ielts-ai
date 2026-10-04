import { useEffect, useState } from 'react'
import ErrorNotebookList from '../components/learning/ErrorNotebookList'
import { acknowledgeMistake, getErrorNotebook } from '../services/errorNotebookApi'

export default function ErrorNotebookPage() {
  const [entries, setEntries] = useState([]); const [error, setError] = useState(''); const [busyId, setBusyId] = useState(null)
  const load = () => getErrorNotebook().then((data) => setEntries(data.entries || [])).catch((cause) => setError(cause.message))
  useEffect(() => { load() }, [])
  const acknowledge = async (id) => { setBusyId(id); setError(''); try { await acknowledgeMistake(id); await load() } catch (cause) { setError(cause.message) } finally { setBusyId(null) } }
  return <main className="error-notebook-page"><div className="diagnostic-page-header"><p className="eyebrow">PHẢN HỒI TỪ BÀI ĐÃ NỘP</p><h1 className="font-display">Sổ lỗi sai</h1><p className="foundation-copy">Xem lại bằng chứng luyện tập thật. Một lần sai không tự động trở thành điểm yếu lặp lại.</p></div>{error ? <div role="alert" className="inline-error">{error}</div> : null}<ErrorNotebookList entries={entries} onAcknowledge={acknowledge} busyId={busyId} /></main>
}
