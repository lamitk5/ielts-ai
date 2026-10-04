import { useState } from 'react'
import { Sparkles, Layers, Award } from 'lucide-react'
import Button from '../../common/Button'

export default function BlueprintSelectionTab({ blueprints = [], selectedBlueprint, onSelect, onExtractFromSource, source }) {
  const [extracting, setExtracting] = useState(false)
  const [targetBand, setTargetBand] = useState('7.5')

  const handleExtract = async () => {
    if (!source) return
    setExtracting(true)
    try {
      const bp = await onExtractFromSource({
        sourceId: source.id,
        passageTitle: source.title,
        targetBand,
      })
      if (bp) onSelect(bp)
    } finally {
      setExtracting(false)
    }
  }

  return (
    <div className="space-y-4">
      {source && (
        <div className="p-3 rounded-lg bg-zinc-900/60 border border-zinc-800 flex items-center justify-between">
          <div className="space-y-0.5">
            <div className="text-[11px] text-zinc-500">Nguồn đã chọn:</div>
            <div className="text-xs font-semibold text-zinc-200">{source.title}</div>
          </div>
          <div className="flex items-center gap-2">
            <select
              value={targetBand}
              onChange={(e) => setTargetBand(e.target.value)}
              className="bg-zinc-950 border border-zinc-700 rounded px-2 py-1 text-xs text-zinc-200"
            >
              <option value="6.5">Band 6.5</option>
              <option value="7.5">Band 7.5</option>
              <option value="8.5">Band 8.5</option>
            </select>
            <Button
              type="button"
              variant="secondary"
              onClick={handleExtract}
              disabled={extracting}
              className="py-1 px-2.5 text-xs flex items-center gap-1.5"
            >
              <Sparkles className="w-3.5 h-3.5 text-amber-400" />
              {extracting ? 'Đang trích xuất...' : 'Trích xuất Blueprint'}
            </Button>
          </div>
        </div>
      )}

      <div className="text-xs font-medium text-zinc-400">Hoặc chọn một Blueprint chuẩn từ danh mục:</div>

      <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
        {blueprints.length === 0 ? (
          <p className="text-xs text-zinc-500 py-4 text-center">Chưa có Blueprint nào.</p>
        ) : (
          blueprints.map((bp) => {
            const isSelected = selectedBlueprint?.id === bp.id
            return (
              <div
                key={bp.id}
                onClick={() => onSelect(bp)}
                className={`p-3 rounded-lg border transition-all cursor-pointer flex items-center justify-between ${
                  isSelected
                    ? 'border-amber-500 bg-amber-500/10'
                    : 'border-zinc-800 bg-zinc-900/40 hover:border-zinc-700'
                }`}
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <Layers className="w-3.5 h-3.5 text-amber-400" />
                    <span className="text-xs font-semibold text-zinc-200">{bp.title}</span>
                  </div>
                  <div className="text-[11px] text-zinc-500">
                    Kỹ năng: {bp.skill} | Band mục tiêu: {bp.targetBand}
                  </div>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="flex items-center gap-1 text-[11px] font-semibold text-amber-300 bg-amber-950/60 border border-amber-800 px-2 py-0.5 rounded">
                    <Award className="w-3 h-3 text-amber-400" />
                    Band {bp.targetBand}
                  </span>
                </div>
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}
