import { Link } from 'react-router-dom'
import { Eye, Clock, CheckCircle, AlertTriangle, ExternalLink } from 'lucide-react'
import Button from '../../common/Button'
import { STATE_LABELS, STATE_BADGE_STYLES } from '../../../features/practice-generator/generatorStateConstants'

export default function GenerationJobTable({ jobs = [], sets = [] }) {
  if (jobs.length === 0 && sets.length === 0) {
    return (
      <div className="p-8 text-center rounded-xl bg-zinc-900/40 border border-zinc-800 space-y-2">
        <p className="text-sm text-zinc-400">Chưa có tác vụ tạo đề nào.</p>
      <p className="text-xs text-zinc-500">Bấm "Tạo bộ đề mới" ở trên để bắt đầu quy trình trích xuất bản thiết kế và tạo đề.</p>
      </div>
    )
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-zinc-800 bg-zinc-900/40">
      <table className="w-full text-left border-collapse text-xs">
        <thead>
          <tr className="border-b border-zinc-800 bg-zinc-950/60 text-zinc-400 font-medium">
            <th className="p-3">Bộ đề / Kỹ năng</th>
            <th className="p-3">Trạng thái</th>
            <th className="p-3">Tiến trình</th>
            <th className="p-3">Thời gian tạo</th>
            <th className="p-3 text-right">Hành động</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-zinc-800/60">
          {sets.map((set) => {
            const badgeClass = STATE_BADGE_STYLES[set.state] || 'bg-zinc-800 text-zinc-300 border-zinc-700'
            const label = STATE_LABELS[set.state] || set.state
            return (
              <tr key={set.id} className="hover:bg-zinc-800/30 transition-colors">
                <td className="p-3">
                  <div className="font-semibold text-zinc-200">{set.title || 'Bộ đề chưa đặt tên'}</div>
                  <div className="text-[11px] text-zinc-500 flex items-center gap-2">
                    <span className="uppercase text-amber-400/80 font-mono">{set.skill}</span>
                    <span>•</span>
                    <span className="font-mono text-zinc-600">ID: {set.id.substring(0, 8)}...</span>
                  </div>
                </td>
                <td className="p-3">
                  <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-medium border ${badgeClass}`}>
                    {label}
                  </span>
                </td>
                <td className="p-3 text-zinc-400">
                  {set.state === 'APPROVED' ? (
                    <span className="text-emerald-400 flex items-center gap-1">
                      <CheckCircle className="w-3.5 h-3.5" />
                      Đã vào ngân hàng đề ({set.publishedSetId || 'curated'})
                    </span>
                  ) : set.state === 'GENERATING' ? (
                    <span className="text-amber-400 animate-pulse flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5" />
                      AI đang tạo nội dung...
                    </span>
                  ) : (
                    <span className="text-zinc-400">Sẵn sàng để quản trị viên duyệt</span>
                  )}
                </td>
                <td className="p-3 text-zinc-500 text-[11px]">
                  {new Date(set.createdAt).toLocaleString()}
                </td>
                <td className="p-3 text-right">
                  <Link to={`/admin/practice-generator/sets/${set.id}`}>
                    <Button variant="secondary" className="py-1 px-2.5 text-xs inline-flex items-center gap-1.5">
                      <Eye className="w-3.5 h-3.5 text-amber-400" />
                      Mở đánh giá
                    </Button>
                  </Link>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
