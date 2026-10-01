import { useState } from 'react'
import { ChevronDown, ChevronUp, CheckCircle, AlertTriangle, XCircle, FileText } from 'lucide-react'
import GlassCard from '../../common/GlassCard'

export default function ValidationReportDrawer({ validationResults = [] }) {
  const [isOpen, setIsOpen] = useState(false)

  const passCount = validationResults.filter((r) => r.status === 'PASS').length
  const warningCount = validationResults.filter((r) => r.status === 'WARNING').length
  const failCount = validationResults.filter((r) => r.status === 'FAIL').length

  return (
    <div className="border border-zinc-800 rounded-xl bg-zinc-950/60 overflow-hidden">
      {/* Drawer toggle header */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="w-full p-3.5 flex items-center justify-between text-left hover:bg-zinc-900/40 transition-colors"
      >
        <div className="flex items-center gap-3">
          <FileText className="w-4 h-4 text-amber-400" />
          <span className="text-xs font-semibold text-zinc-200 font-mono uppercase">
            Báo cáo kiểm định chất lượng đa tầng ({validationResults.length} Tiêu chuẩn)
          </span>
          <div className="flex items-center gap-2 text-[11px]">
            <span className="text-emerald-400 font-medium">✓ {passCount} Đạt</span>
            {warningCount > 0 && <span className="text-amber-400 font-medium">⚠ {warningCount} Cảnh báo</span>}
            {failCount > 0 && <span className="text-rose-400 font-medium">✗ {failCount} Thất bại</span>}
          </div>
        </div>

        <div className="text-zinc-400">
          {isOpen ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
        </div>
      </button>

      {/* Drawer content */}
      {isOpen && (
        <div className="p-4 border-t border-zinc-800/80 space-y-3 bg-zinc-900/30 text-xs">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {validationResults.map((r, idx) => {
              const findings = typeof r.findings === 'string' ? JSON.parse(r.findings) : r.findings || []

              return (
                <div
                  key={r.id || idx}
                  className="p-3 rounded-lg border border-zinc-800/80 bg-zinc-950/50 space-y-1.5"
                >
                  <div className="flex items-center justify-between">
                    <span className="font-mono font-bold text-zinc-300">{r.validatorName}</span>
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        r.status === 'PASS'
                          ? 'bg-emerald-950/60 text-emerald-300 border border-emerald-800'
                          : r.status === 'WARNING'
                          ? 'bg-amber-950/60 text-amber-300 border border-amber-800'
                          : 'bg-rose-950/60 text-rose-300 border border-rose-800'
                      }`}
                    >
                      {r.status}
                    </span>
                  </div>

                  {findings.length > 0 ? (
                    <ul className="list-disc list-inside text-[11px] text-zinc-400 space-y-0.5">
                      {findings.map((f, fIdx) => (
                        <li key={fIdx} className="truncate">
                          {typeof f === 'string' ? f : f.message || f.description || JSON.stringify(f)}
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="text-[11px] text-emerald-400/80 italic">Không phát hiện sai sót.</p>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
