import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, RefreshCw, AlertCircle } from 'lucide-react'
import PageContainer from '../components/common/PageContainer'
import SectionTitle from '../components/common/SectionTitle'
import Button from '../components/common/Button'
import SkeletonBlock from '../components/common/SkeletonBlock'
import PracticeReviewCanvas from '../components/admin/review/PracticeReviewCanvas'
import { usePracticeReviewSet } from '../features/practice-generator/usePracticeReviewSet'

export default function AdminPracticeReviewPage() {
  const { setId } = useParams()
  const {
    payload,
    loading,
    error,
    submitting,
    comparison,
    refresh,
    submitReview,
    regenerateItem,
    compareVersions,
  } = usePracticeReviewSet(setId)

  const handleApprove = async () => {
    await submitReview({ action: 'APPROVE', feedbackNotes: 'Phê duyệt bởi biên tập viên' })
  }

  const handleRequestRevision = async (notes) => {
    await submitReview({ action: 'REQUEST_REVISION', revisionInstructions: notes })
  }

  const handleReject = async () => {
    await submitReview({ action: 'REJECT', feedbackNotes: 'Từ chối bởi biên tập viên' })
  }

  const handleRegenerateQuestion = async ({ questionId, revisionInstructions }) => {
    await regenerateItem({ questionId, revisionInstructions })
  }

  return (
    <PageContainer className="py-6 space-y-6">
      {/* Top navigation header */}
      <div className="flex items-center justify-between border-b border-zinc-800/80 pb-4">
        <div className="flex items-center gap-3">
          <Link to="/admin/generator">
            <Button variant="secondary" className="p-2 text-xs">
              <ArrowLeft className="w-4 h-4" />
            </Button>
          </Link>
          <div>
            <div className="text-[11px] font-mono uppercase text-amber-400">Không gian Kiểm duyệt Bài tập</div>
            <h2 className="text-base font-serif font-bold text-zinc-100">
              {payload?.practiceSet?.title || 'Kiểm duyệt Đề thi AI'}
            </h2>
          </div>
        </div>

        <Button
          variant="secondary"
          onClick={refresh}
          disabled={loading || submitting}
          className="py-1.5 px-3 text-xs flex items-center gap-1.5"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </Button>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-800 text-rose-300 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {loading && !payload ? (
        <SkeletonBlock className="h-96 w-full rounded-2xl" label="Đang tải dữ liệu kiểm duyệt..." />
      ) : (
        <PracticeReviewCanvas
          payload={payload}
          onApprove={handleApprove}
          onRequestRevision={handleRequestRevision}
          onReject={handleReject}
          onRegenerateQuestion={handleRegenerateQuestion}
          onCompareVersions={compareVersions}
          comparison={comparison}
          submitting={submitting}
        />
      )}
    </PageContainer>
  )
}
