import { useState } from 'react'
import { HelpCircle, RefreshCw, Key, Search, Sparkles } from 'lucide-react'
import Button from '../../common/Button'
import GlassCard from '../../common/GlassCard'

export default function QuestionReviewPane({
  questions = [],
  onHoverQuestion,
  onRegenerateQuestion,
  regenerating,
}) {
  const [targetRegenId, setTargetRegenId] = useState(null)
  const [instructions, setInstructions] = useState('')

  const handleStartRegen = (qId) => {
    setTargetRegenId(qId)
    setInstructions('')
  }

  const handleConfirmRegen = async (qId) => {
    if (!onRegenerateQuestion) return
    await onRegenerateQuestion({
      questionId: qId,
      revisionInstructions: instructions.trim() || undefined,
    })
    setTargetRegenId(null)
  }

  return (
    <GlassCard className="h-full flex flex-col p-5 border-zinc-800 bg-zinc-900/50">
      <div className="flex items-center justify-between border-b border-zinc-800 pb-3 mb-4">
        <div className="flex items-center gap-2">
          <HelpCircle className="w-4 h-4 text-amber-400" />
          <h4 className="text-sm font-serif font-bold text-zinc-100">Bộ câu hỏi ({questions.length})</h4>
        </div>
        <span className="text-[11px] font-mono text-zinc-500">Định dạng chuẩn IELTS</span>
      </div>

      <div className="flex-1 overflow-y-auto space-y-4 pr-2 text-xs">
        {questions.map((q, idx) => {
          const isRegenOpen = targetRegenId === q.id

          return (
            <div
              key={q.id || idx}
              onMouseEnter={() => onHoverQuestion?.(q)}
              onMouseLeave={() => onHoverQuestion?.(null)}
              className="p-4 rounded-xl border border-zinc-800/80 bg-zinc-950/60 hover:border-zinc-700 transition-all space-y-3"
            >
              {/* Question header */}
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-xs font-bold text-amber-400">
                    #{idx + 1}
                  </span>
                  <span className="px-2 py-0.5 rounded text-[10px] font-medium bg-zinc-900 border border-zinc-700 text-zinc-300">
                    {q.taskType}
                  </span>
                </div>

                <Button
                  variant="secondary"
                  onClick={() => handleStartRegen(q.id)}
                  disabled={regenerating}
                  className="py-1 px-2 text-[11px] flex items-center gap-1"
                >
                  <RefreshCw className={`w-3 h-3 text-amber-400 ${regenerating ? 'animate-spin' : ''}`} />
                  Hiệu chỉnh AI
                </Button>
              </div>

              {/* Prompt */}
              <p className="font-medium text-zinc-200 text-xs leading-relaxed">{q.prompt}</p>

              {/* Options */}
              {q.options && q.options.length > 0 && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5 pt-1">
                  {q.options.map((opt, optIdx) => {
                    const optKey = String.fromCharCode(65 + optIdx)
                    const isCorrect = q.answerKey?.trim().toUpperCase() === optKey

                    return (
                      <div
                        key={optIdx}
                        className={`px-2.5 py-1.5 rounded text-[11px] flex items-center gap-2 border ${
                          isCorrect
                            ? 'bg-emerald-950/40 border-emerald-800/80 text-emerald-300'
                            : 'bg-zinc-900/60 border-zinc-800 text-zinc-400'
                        }`}
                      >
                        <span className="font-mono font-bold text-zinc-500">{optKey}.</span>
                        <span>{opt}</span>
                      </div>
                    )
                  })}
                </div>
              )}

              {/* Answer Key & Evidence */}
              <div className="flex flex-wrap items-center gap-3 pt-2 border-t border-zinc-800/60 text-[11px]">
                <div className="flex items-center gap-1.5 text-emerald-400 font-semibold font-mono">
                  <Key className="w-3.5 h-3.5" />
                  <span>Đáp án: {q.answerKey}</span>
                </div>

                {q.evidenceSpan && (
                  <div className="flex items-center gap-1 text-amber-300/90 italic truncate max-w-[260px]">
                    <Search className="w-3 h-3 shrink-0 text-amber-400" />
                    <span className="truncate">"{q.evidenceSpan}"</span>
                  </div>
                )}
              </div>

              {/* Targeted item regeneration form */}
              {isRegenOpen && (
                <div className="p-3 rounded-lg bg-zinc-900 border border-amber-500/50 space-y-2 animate-in fade-in duration-150">
                  <div className="text-[11px] font-semibold text-amber-300 flex items-center gap-1">
                    <Sparkles className="w-3.5 h-3.5" />
                    Hiệu chỉnh mục câu hỏi #{idx + 1}
                  </div>
                  <input
                    type="text"
                    value={instructions}
                    onChange={(e) => setInstructions(e.target.value)}
                    placeholder="Chỉ dẫn (VD: Làm rõ chi tiết về năm xuất bản, tránh bẫy gây tranh cãi...)"
                    className="w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1 text-xs text-zinc-200 focus:outline-none focus:border-amber-500"
                  />
                  <div className="flex justify-end gap-2">
                    <Button
                      variant="secondary"
                      onClick={() => setTargetRegenId(null)}
                      className="py-1 px-2 text-[11px]"
                    >
                      Hủy
                    </Button>
                    <Button
                      onClick={() => handleConfirmRegen(q.id)}
                      disabled={regenerating}
                      className="py-1 px-3 text-[11px] font-bold bg-amber-500 hover:bg-amber-400 text-zinc-950"
                    >
                      {regenerating ? 'Đang tạo lại...' : 'Tạo lại câu này'}
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )
        })}
      </div>
    </GlassCard>
  )
}
