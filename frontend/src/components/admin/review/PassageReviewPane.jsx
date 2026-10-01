import { useState } from 'react'
import { BookOpen, Highlighter } from 'lucide-react'
import GlassCard from '../../common/GlassCard'

export default function PassageReviewPane({ passage, activeEvidence, hoveredQuestionId }) {
  const title = passage?.title || 'Đoạn văn đọc IELTS'
  const paragraphs = passage?.paragraphs || []

  return (
    <GlassCard className="h-full flex flex-col p-5 border-zinc-800 bg-zinc-900/50">
      <div className="flex items-center justify-between border-b border-zinc-800 pb-3 mb-4">
        <div className="flex items-center gap-2">
          <BookOpen className="w-4 h-4 text-amber-400" />
          <h4 className="text-sm font-serif font-bold text-zinc-100">{title}</h4>
        </div>
        <span className="text-[11px] font-mono text-zinc-500">{paragraphs.length} Đoạn văn</span>
      </div>

      <div className="flex-1 overflow-y-auto space-y-4 pr-2 text-xs leading-relaxed text-zinc-300 font-serif">
        {paragraphs.map((p, idx) => {
          const pLabel = String.fromCharCode(65 + idx) // A, B, C...
          const isHighlighted =
            activeEvidence &&
            activeEvidence.trim() &&
            p.text &&
            p.text.toLowerCase().includes(activeEvidence.toLowerCase().trim())

          return (
            <div
              key={p.id || idx}
              className={`p-3 rounded-lg border transition-all ${
                isHighlighted
                  ? 'bg-amber-500/10 border-amber-500/60 shadow-lg shadow-amber-500/5'
                  : 'bg-zinc-950/40 border-zinc-800/60'
              }`}
            >
              <div className="flex items-start gap-2.5">
                <span className="font-mono text-[11px] font-bold text-amber-400/80 bg-amber-950/60 border border-amber-800/60 px-1.5 py-0.5 rounded shrink-0">
                  {pLabel}
                </span>
                <p className="text-zinc-300 select-text leading-6">
                  {p.text}
                </p>
              </div>
            </div>
          )
        })}
      </div>
    </GlassCard>
  )
}
