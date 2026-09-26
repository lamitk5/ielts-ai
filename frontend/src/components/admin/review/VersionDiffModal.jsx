import { useState } from 'react'
import { X, GitCompare, ArrowRight } from 'lucide-react'
import Button from '../../common/Button'
import GlassCard from '../../common/GlassCard'

export default function VersionDiffModal({
  isOpen,
  onClose,
  versionHistory = [],
  onCompare,
  comparison,
}) {
  const [v1, setV1] = useState(1)
  const [v2, setV2] = useState(versionHistory.length || 1)
  const [comparing, setComparing] = useState(false)

  if (!isOpen) return null

  const handleRunCompare = async () => {
    setComparing(true)
    try {
      await onCompare(v1, v2)
    } finally {
      setComparing(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-3xl">
        <GlassCard className="p-6 border border-zinc-700 shadow-2xl space-y-5">
          <div className="flex items-center justify-between border-b border-zinc-800 pb-3">
            <div className="flex items-center gap-2">
              <GitCompare className="w-4 h-4 text-amber-400" />
              <h3 className="text-base font-serif font-bold text-zinc-100">So sánh Lịch sử Phiên bản</h3>
            </div>
            <button
              onClick={onClose}
              className="p-1 rounded text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800"
              aria-label="Đóng"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <div className="flex items-center gap-4 bg-zinc-950 p-3 rounded-xl border border-zinc-800">
            <div className="flex items-center gap-2 text-xs">
              <span className="text-zinc-400">Phiên bản gốc:</span>
              <select
                value={v1}
                onChange={(e) => setV1(Number(e.target.value))}
                className="bg-zinc-900 border border-zinc-700 rounded px-2 py-1 text-zinc-200 text-xs"
              >
                {versionHistory.map((v) => (
                  <option key={v.versionNumber} value={v.versionNumber}>
                    v{v.versionNumber} ({new Date(v.createdAt).toLocaleDateString()})
                  </option>
                ))}
              </select>
            </div>

            <ArrowRight className="w-4 h-4 text-zinc-600" />

            <div className="flex items-center gap-2 text-xs">
              <span className="text-zinc-400">So sánh với:</span>
              <select
                value={v2}
                onChange={(e) => setV2(Number(e.target.value))}
                className="bg-zinc-900 border border-zinc-700 rounded px-2 py-1 text-zinc-200 text-xs"
              >
                {versionHistory.map((v) => (
                  <option key={v.versionNumber} value={v.versionNumber}>
                    v{v.versionNumber} ({new Date(v.createdAt).toLocaleDateString()})
                  </option>
                ))}
              </select>
            </div>

            <Button
              onClick={handleRunCompare}
              disabled={comparing || v1 === v2}
              className="ml-auto py-1 px-3 text-xs"
            >
              {comparing ? 'Đang so sánh...' : 'Thực hiện So sánh'}
            </Button>
          </div>

          {comparison ? (
            <div className="space-y-3 max-h-72 overflow-y-auto text-xs">
              <div className="p-3 rounded-lg bg-zinc-950/60 border border-zinc-800 space-y-2">
                <div className="font-semibold text-amber-300">Kết quả khác biệt giữa v{v1} và v{v2}:</div>
                <div className="grid grid-cols-2 gap-2 text-zinc-400 text-[11px]">
                  <div>
                    Đoạn văn:{' '}
                    <span className={comparison.passageChanged ? 'text-amber-400' : 'text-emerald-400'}>
                      {comparison.passageChanged ? 'Có thay đổi' : 'Không đổi'}
                    </span>
                  </div>
                  <div>
                    Số lượng câu hỏi: {comparison.baseQuestionCount} &rarr; {comparison.compareQuestionCount}
                  </div>
                </div>

                {comparison.questionDifferences && comparison.questionDifferences.length > 0 && (
                  <div className="pt-2 border-t border-zinc-800 space-y-1">
                    <span className="text-[11px] font-semibold text-zinc-300">Chi tiết thay đổi câu hỏi:</span>
                    <ul className="list-disc list-inside text-[11px] text-zinc-400 space-y-0.5">
                      {comparison.questionDifferences.map((d, idx) => (
                        <li key={idx}>{d}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <p className="text-xs text-zinc-500 text-center py-4">Chọn 2 phiên bản và bấm "Thực hiện So sánh".</p>
          )}

          <div className="flex justify-end pt-2 border-t border-zinc-800">
            <Button variant="secondary" onClick={onClose} className="py-1.5 px-4 text-xs">
              Đóng
            </Button>
          </div>
        </GlassCard>
      </div>
    </div>
  )
}
