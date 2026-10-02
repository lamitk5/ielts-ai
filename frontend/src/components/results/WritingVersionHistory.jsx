import React from 'react'

export default function WritingVersionHistory({
  versions = [],
  activeVersionId,
  onSelectVersion,
  onCompare,
}) {
  if (!versions || versions.length === 0) {
    return null
  }

  return (
    <div className="rounded-2xl border border-amber-500/20 bg-slate-900/60 p-4 backdrop-blur-md">
      <div className="flex items-center justify-between border-b border-amber-500/10 pb-3 mb-3">
        <h3 className="font-serif text-lg font-semibold text-amber-200">
          Lịch sử phiên bản ({versions.length})
        </h3>
        {versions.length >= 2 && onCompare && (
          <button
            type="button"
            onClick={() => onCompare(versions[1].versionNumber, versions[0].versionNumber)}
            className="rounded-lg bg-amber-500/10 px-3 py-1 text-xs font-medium text-amber-300 transition hover:bg-amber-500/20"
          >
            So sánh v{versions[1].versionNumber} & v{versions[0].versionNumber}
          </button>
        )}
      </div>

      <div className="space-y-2 max-h-60 overflow-y-auto">
        {versions.map((ver) => {
          const isActive = ver.id === activeVersionId
          return (
            <div
              key={ver.id}
              onClick={() => onSelectVersion && onSelectVersion(ver)}
              className={`flex cursor-pointer items-center justify-between rounded-xl p-3 text-sm transition ${
                isActive
                  ? 'border border-amber-500/40 bg-amber-500/15 text-amber-100'
                  : 'border border-transparent bg-slate-800/40 text-slate-300 hover:bg-slate-800/80 hover:text-slate-100'
              }`}
            >
              <div className="flex items-center space-x-2">
                <span className="font-semibold text-amber-400">v{ver.versionNumber}</span>
                <span className="text-xs text-slate-400">
                  {ver.wordCount} từ
                </span>
                {ver.parentVersionId && (
                  <span className="text-xs text-slate-500">(chỉnh sửa từ v{ver.versionNumber - 1})</span>
                )}
              </div>
              <span className="text-xs text-slate-500">
                {new Date(ver.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
              </span>
            </div>
          )
        })}
      </div>
    </div>
  )
}
