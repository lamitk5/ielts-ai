import { useCallback, useEffect, useMemo, useState } from 'react'
import SectionTitle from '../components/common/SectionTitle'
import SkeletonBlock from '../components/common/SkeletonBlock'
import RagAdminUnlock from '../components/admin/RagAdminUnlock'
import RagDocumentDetail from '../components/admin/RagDocumentDetail'
import RagDocumentTable from '../components/admin/RagDocumentTable'
import RagJobList from '../components/admin/RagJobList'
import RagUploadForm from '../components/admin/RagUploadForm'
import { createRagAdminApi, readAdminToken } from '../services/ragAdminApi'
import { useAuth } from '../features/auth/AuthProvider'

function AdminRagPage() {
  const [token, setToken] = useState(() => readAdminToken())
  const [documents, setDocuments] = useState([])
  const [jobs, setJobs] = useState([])
  const [detail, setDetail] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const api = useMemo(() => createRagAdminApi(), [])
  const { user } = useAuth()
  const isAuthenticatedAdmin = user?.role === 'ADMIN'

  const refresh = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [nextDocuments, nextJobs] = await Promise.all([api.list(), api.jobs()])
      setDocuments(nextDocuments)
      setJobs(nextJobs)
    } catch (requestError) {
      setError(requestError.message)
    } finally { setLoading(false) }
  }, [api])

  useEffect(() => { if (token || isAuthenticatedAdmin) refresh() }, [isAuthenticatedAdmin, refresh, token])

  if (!token && !isAuthenticatedAdmin) return <RagAdminUnlock onUnlock={setToken} />

  async function upload(metadata, file) {
    try { await api.upload(metadata, file); await refresh() }
    catch (requestError) { setError(requestError.message) }
  }
  async function action(document, actionName) {
    try { await api.action(document.id, actionName, actionName === 'approve' ? 'Reviewed in local admin CMS' : undefined); await refresh() }
    catch (requestError) { setError(requestError.message) }
  }
  async function select(id) {
    try { setDetail(await api.detail(id)) }
    catch (requestError) { setError(requestError.message) }
  }

  return <section className="admin-page" aria-labelledby="admin-title">
    <div className="admin-page-header"><SectionTitle eyebrow="RAG ADMIN CMS" title="Quản trị học liệu IELTS" description="Upload, review và kiểm soát nguồn trước khi đưa vào Tutor." /></div>
    {error ? <p role="alert" className="admin-error">{error}</p> : null}
    <div className="admin-layout"><RagUploadForm onUpload={upload} /><RagDocumentDetail detail={detail} /><RagJobList jobs={jobs} /></div>
    {loading ? <SkeletonBlock className="admin-loading" label="Đang tải danh sách học liệu" /> : <RagDocumentTable documents={documents} onSelect={select} onAction={action} />}
  </section>
}

export default AdminRagPage
