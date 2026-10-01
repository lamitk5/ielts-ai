import { useState } from 'react'
import { AlertTriangle, CheckCircle2, FileText, Upload } from 'lucide-react'
import Button from '../../common/Button'
import { RIGHTS_STATUSES } from '../../../features/practice-generator/generatorStateConstants'

export default function SourceRegistrationTab({ onRegistered, onSelectExisting, sources = [] }) {
  const [mode, setMode] = useState('select') // 'select' | 'new'
  const [title, setTitle] = useState('')
  const [rawText, setRawText] = useState('')
  const [rightsStatus, setRightsStatus] = useState(RIGHTS_STATUSES.APPROVED)
  const [skill, setSkill] = useState('READING')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const wordCount = rawText.trim() ? rawText.trim().split(/\s+/).length : 0
  const isApproved = rightsStatus === RIGHTS_STATUSES.APPROVED

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!title.trim() || !rawText.trim()) {
      setError('Vui lòng nhập tiêu đề và nội dung bài đọc.')
      return
    }
    setError('')
    setSubmitting(true)
    try {
      const source = await onRegistered({
        title: title.trim(),
        rawText: rawText.trim(),
        rightsStatus,
        skill,
        language: 'en',
      })
      if (source) {
        onSelectExisting(source)
      }
    } catch (err) {
      setError(err.message || 'Không thể đăng ký nguồn tài liệu.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex gap-2 p-1 bg-zinc-900/80 rounded-lg border border-zinc-800">
        <button
          type="button"
          onClick={() => setMode('select')}
          className={`flex-1 py-1.5 text-xs font-medium rounded-md transition-colors ${
            mode === 'select'
              ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
              : 'text-zinc-400 hover:text-zinc-200'
          }`}
        >
          Chọn nguồn đã duyệt ({sources.filter((s) => s.rightsStatus === 'APPROVED').length})
        </button>
        <button
          type="button"
          onClick={() => setMode('new')}
          className={`flex-1 py-1.5 text-xs font-medium rounded-md transition-colors ${
            mode === 'new'
              ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
              : 'text-zinc-400 hover:text-zinc-200'
          }`}
        >
          Đăng ký nguồn mới
        </button>
      </div>

      {mode === 'select' ? (
        <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
          {sources.length === 0 ? (
            <p className="text-xs text-zinc-500 py-4 text-center">Chưa có nguồn tài liệu nào. Hãy đăng ký nguồn mới.</p>
          ) : (
            sources.map((s) => (
              <div
                key={s.id}
                onClick={() => s.rightsStatus === 'APPROVED' && onSelectExisting(s)}
                className={`p-3 rounded-lg border transition-all cursor-pointer flex items-center justify-between ${
                  s.rightsStatus === 'APPROVED'
                    ? 'border-zinc-800 hover:border-amber-500/50 hover:bg-amber-500/5 bg-zinc-900/40'
                    : 'border-zinc-800/40 bg-zinc-950/40 opacity-60 cursor-not-allowed'
                }`}
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <FileText className="w-3.5 h-3.5 text-amber-400" />
                    <span className="text-xs font-semibold text-zinc-200">{s.title}</span>
                  </div>
                  <div className="text-[11px] text-zinc-500">
                    Checksum: {s.checksum?.substring(0, 10)}... | Tạo lúc: {new Date(s.createdAt).toLocaleDateString()}
                  </div>
                </div>
                <div>
                  <span
                    className={`text-[10px] px-2 py-0.5 rounded-full border ${
                      s.rightsStatus === 'APPROVED'
                        ? 'bg-emerald-950/60 text-emerald-300 border-emerald-800'
                        : 'bg-rose-950/60 text-rose-300 border-rose-800'
                    }`}
                  >
                    {s.rightsStatus}
                  </span>
                </div>
              </div>
            ))
          )}
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-3">
          {error && <p className="text-xs text-rose-400 bg-rose-950/30 p-2 rounded border border-rose-900">{error}</p>}

          <div>
            <label className="block text-xs text-zinc-400 mb-1">Tiêu đề tài liệu nguồn</label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="VD: The History of Polar Exploration"
              className="w-full bg-zinc-950 border border-zinc-800 rounded px-3 py-1.5 text-xs text-zinc-200 focus:outline-none focus:border-amber-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-zinc-400 mb-1">Trạng thái bản quyền (Rights Status)</label>
              <select
                value={rightsStatus}
                onChange={(e) => setRightsStatus(e.target.value)}
                className="w-full bg-zinc-950 border border-zinc-800 rounded px-3 py-1.5 text-xs text-zinc-200 focus:outline-none focus:border-amber-500"
              >
                <option value={RIGHTS_STATUSES.APPROVED}>APPROVED (Đã duyệt bản quyền)</option>
                <option value={RIGHTS_STATUSES.PENDING_REVIEW}>PENDING_REVIEW (Chờ duyệt)</option>
                <option value={RIGHTS_STATUSES.RESTRICTED}>RESTRICTED (Hạn chế)</option>
                <option value={RIGHTS_STATUSES.REJECTED}>REJECTED (Từ chối)</option>
              </select>
            </div>
            <div>
              <label className="block text-xs text-zinc-400 mb-1">Kỹ năng</label>
              <select
                value={skill}
                onChange={(e) => setSkill(e.target.value)}
                className="w-full bg-zinc-950 border border-zinc-800 rounded px-3 py-1.5 text-xs text-zinc-200 focus:outline-none focus:border-amber-500"
              >
                <option value="READING">Reading</option>
                <option value="LISTENING">Listening</option>
              </select>
            </div>
          </div>

          {!isApproved && (
            <div className="flex items-start gap-2 p-2.5 rounded-lg bg-amber-950/40 border border-amber-800 text-amber-300 text-[11px]">
              <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5 text-amber-400" />
              <span>
                Cảnh báo: Chỉ tài liệu ở trạng thái <strong>APPROVED</strong> mới có thể kích hoạt bộ tạo đề. Tài liệu
                này sẽ được lưu nhưng chưa thể tạo đề ngay.
              </span>
            </div>
          )}

          <div>
            <div className="flex justify-between items-center mb-1">
              <label className="text-xs text-zinc-400">Nội dung văn bản (400 - 1200 từ)</label>
              <span className={`text-[11px] ${wordCount >= 400 && wordCount <= 1200 ? 'text-emerald-400' : 'text-zinc-500'}`}>
                {wordCount} từ
              </span>
            </div>
            <textarea
              rows={5}
              value={rawText}
              onChange={(e) => setRawText(e.target.value)}
              placeholder="Dán nội dung bài đọc gốc vào đây..."
              className="w-full bg-zinc-950 border border-zinc-800 rounded p-2 text-xs text-zinc-200 font-mono focus:outline-none focus:border-amber-500"
            />
          </div>

          <Button type="submit" disabled={submitting || !title || !rawText} className="w-full py-1.5 text-xs">
            {submitting ? 'Đang lưu...' : 'Lưu và Sử dụng Nguồn này'}
          </Button>
        </form>
      )}
    </div>
  )
}
