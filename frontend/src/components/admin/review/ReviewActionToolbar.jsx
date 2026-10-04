import { useState } from 'react'
import { CheckCircle2, RotateCcw, XCircle, GitCompare, Sparkles } from 'lucide-react'
import Button from '../../common/Button'
import { STATE_LABELS, STATE_BADGE_STYLES } from '../../../features/practice-generator/generatorStateConstants'

export default function ReviewActionToolbar({
  state,
  versionNumber = 1,
  onApprove,
  onRequestRevision,
  onReject,
  onOpenDiff,
  submitting,
}) {
  const [isRevisionModalOpen, setIsRevisionModalOpen] = useState(false)
  const [revisionNotes, setRevisionNotes] = useState('')

  const handleConfirmRevision = async () => {
    await onRequestRevision(revisionNotes)
    setIsRevisionModalOpen(false)
    setRevisionNotes('')
  }

  const badgeClass = STATE_BADGE_STYLES[state] || 'bg-zinc-800 text-zinc-300'
  const isApproved = state === 'APPROVED'

  return (
    <div className="sticky bottom-4 z-40 p-4 rounded-2xl bg-zinc-950/90 border border-zinc-700/80 backdrop-blur-md shadow-2xl flex flex-col sm:flex-row items-center justify-between gap-4">
      {/* State & Version info */}
      <div className="flex items-center gap-3">
        <span className={`px-2.5 py-1 rounded-full text-xs font-semibold border ${badgeClass}`}>
          {STATE_LABELS[state] || state}
        </span>
        <span className="font-mono text-xs text-zinc-400 bg-zinc-900 px-2 py-1 rounded border border-zinc-800">
          Phiên bản: v{versionNumber}
        </span>
        <Button
          variant="secondary"
          onClick={onOpenDiff}
          className="py-1 px-2.5 text-xs flex items-center gap-1.5"
        >
          <GitCompare className="w-3.5 h-3.5 text-zinc-400" />
          So sánh phiên bản
        </Button>
      </div>

      {/* Review Actions */}
      <div className="flex items-center gap-2.5">
        {!isApproved && (
          <>
            <Button
              variant="secondary"
              onClick={onReject}
              disabled={submitting}
              className="py-1.5 px-3 text-xs text-rose-400 hover:text-rose-300 hover:bg-rose-950/30 border-rose-900/50 flex items-center gap-1"
            >
              <XCircle className="w-3.5 h-3.5" />
              Từ chối
            </Button>

            <Button
              variant="secondary"
              onClick={() => setIsRevisionModalOpen(true)}
              disabled={submitting}
              className="py-1.5 px-3 text-xs text-amber-400 hover:text-amber-300 hover:bg-amber-950/30 border-amber-900/50 flex items-center gap-1"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              Yêu cầu hiệu chỉnh
            </Button>

            <Button
              onClick={onApprove}
              disabled={submitting}
              className="py-1.5 px-5 text-xs font-bold bg-gradient-to-r from-amber-500 to-amber-400 text-zinc-950 hover:from-amber-400 hover:to-amber-300 shadow-lg shadow-amber-500/20 flex items-center gap-1.5"
            >
              <CheckCircle2 className="w-4 h-4" />
              {submitting ? 'Đang duyệt...' : 'Phê duyệt & Xuất bản'}
            </Button>
          </>
        )}

        {isApproved && (
          <div className="flex items-center gap-1.5 text-xs font-bold text-emerald-400 bg-emerald-950/40 border border-emerald-800 px-3 py-1.5 rounded-lg">
            <CheckCircle2 className="w-4 h-4" />
            Đã phát hành vào Ngân hàng Đề thi
          </div>
        )}
      </div>

      {/* Revision notes popup */}
      {isRevisionModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-zinc-900 border border-zinc-700 p-5 rounded-xl space-y-4 shadow-2xl">
            <h4 className="text-sm font-bold text-zinc-100 flex items-center gap-1.5">
              <RotateCcw className="w-4 h-4 text-amber-400" />
              Yêu cầu hiệu chỉnh bộ đề
            </h4>
            <textarea
              rows={4}
              value={revisionNotes}
              onChange={(e) => setRevisionNotes(e.target.value)}
              placeholder="Nhập ghi chú yêu cầu hiệu chỉnh cho biên tập viên hoặc AI..."
              className="w-full bg-zinc-950 border border-zinc-700 rounded p-2 text-xs text-zinc-200 focus:outline-none focus:border-amber-500"
            />
            <div className="flex justify-end gap-2">
              <Button
                variant="secondary"
                onClick={() => setIsRevisionModalOpen(false)}
                className="py-1 px-3 text-xs"
              >
                Hủy
              </Button>
              <Button
                onClick={handleConfirmRevision}
                disabled={submitting}
                className="py-1 px-3 text-xs bg-amber-500 hover:bg-amber-400 text-zinc-950 font-bold"
              >
                Gửi yêu cầu
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
