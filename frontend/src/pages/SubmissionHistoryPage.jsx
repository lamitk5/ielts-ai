import { useEffect, useState } from 'react'
import PageContainer from '../components/common/PageContainer'
import SubmissionHistoryList from '../components/results/SubmissionHistoryList'
import { getSubmissionHistory } from '../services/resultsApi'

export default function SubmissionHistoryPage() {
  const [state, setState] = useState({ loading: true, items: [], error: '' })
  useEffect(() => {
    getSubmissionHistory().then((payload) => setState({ loading: false, items: payload?.items ?? [], error: '' }))
      .catch((error) => setState({ loading: false, items: [], error: error.message }))
  }, [])
  return <PageContainer className="py-8 space-y-6"><div><p className="eyebrow">TIẾN ĐỘ HỌC TẬP</p><h1 className="font-display">Lịch sử bài làm</h1><p>Xem lại các lượt luyện tập và kết quả đã được lưu.</p></div>{state.loading ? <p role="status">Đang tải lịch sử bài làm…</p> : state.error ? <p role="alert">{state.error}</p> : <SubmissionHistoryList items={state.items} />}</PageContainer>
}
