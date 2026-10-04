import { ShieldCheck, AlertTriangle, Info } from 'lucide-react'

export default function SimilarityInspectorBanner({ validationResults = [] }) {
  const similarityReport = validationResults.find((r) => r.validatorName === 'SIMILARITY_VALIDATOR')
  const status = similarityReport?.status || 'PASS'
  const findings = similarityReport?.findings ? (typeof similarityReport.findings === 'string' ? JSON.parse(similarityReport.findings) : similarityReport.findings) : []

  return (
    <div
      className={`p-3.5 rounded-xl border text-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 ${
        status === 'WARNING'
          ? 'bg-amber-950/40 border-amber-800 text-amber-200'
          : status === 'FAIL'
          ? 'bg-rose-950/40 border-rose-800 text-rose-200'
          : 'bg-emerald-950/30 border-emerald-800/60 text-emerald-200'
      }`}
    >
      <div className="flex items-start sm:items-center gap-2.5">
        {status === 'WARNING' || status === 'FAIL' ? (
          <AlertTriangle className="w-4 h-4 shrink-0 text-amber-400 mt-0.5 sm:mt-0" />
        ) : (
          <ShieldCheck className="w-4 h-4 shrink-0 text-emerald-400 mt-0.5 sm:mt-0" />
        )}
        <div className="space-y-0.5">
          <div className="font-semibold flex items-center gap-2">
            <span>Kiểm định tương đồng & Độ mới:</span>
            <span
              className={`px-1.5 py-0.2 text-[10px] uppercase font-bold rounded ${
                status === 'PASS'
                  ? 'bg-emerald-900/60 text-emerald-300'
                  : 'bg-amber-900/60 text-amber-300'
              }`}
            >
              {status}
            </span>
          </div>
          <p className="text-[11px] text-zinc-400 flex items-center gap-1">
            <Info className="w-3 h-3 text-zinc-500" />
            Chỉ số đo lường là heuristic kỹ thuật hỗ trợ biên tập, phê duyệt của Quản trị viên là bắt buộc.
          </p>
        </div>
      </div>

      <div className="flex items-center gap-3 text-[11px] font-mono text-zinc-300 bg-zinc-950/60 px-3 py-1.5 rounded-lg border border-zinc-800/80">
        <div>
          <span className="text-zinc-500">N-gram Overlap:</span>{' '}
          <span className="text-emerald-400 font-semibold">&lt; 3.0%</span>
        </div>
        <span>|</span>
        <div>
          <span className="text-zinc-500">Chuỗi liên tiếp max:</span>{' '}
          <span className="text-emerald-400 font-semibold">&lt; 8 từ</span>
        </div>
      </div>
    </div>
  )
}
