import React, { useState } from 'react'
import WritingVersionHistory from './WritingVersionHistory.jsx'

export default function WritingResultShell({
  evaluation,
  versions = [],
  activeVersion,
  onSelectVersion,
  onRetryEvaluation,
  comparisonData,
  onCompareVersions,
  isLoading = false,
  error = null,
}) {
  const [showComparison, setShowComparison] = useState(false)

  if (isLoading) {
    return (
      <div className="animate-pulse space-y-6 rounded-3xl border border-amber-500/20 bg-slate-900/80 p-6 backdrop-blur-xl">
        <div className="h-8 w-48 rounded-lg bg-amber-500/10" />
        <div className="h-24 w-full rounded-2xl bg-slate-800/40" />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="h-32 rounded-2xl bg-slate-800/40" />
          <div className="h-32 rounded-2xl bg-slate-800/40" />
        </div>
      </div>
    )
  }

  if (error || (evaluation && evaluation.status === 'FAILED')) {
    return (
      <div className="rounded-3xl border border-rose-500/30 bg-rose-950/20 p-6 text-slate-100 backdrop-blur-xl">
        <div className="flex items-center space-x-3 text-rose-400">
          <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <h3 className="font-serif text-lg font-semibold">Đánh giá AI chưa hoàn tất</h3>
        </div>
        <p className="mt-2 text-sm text-slate-300">
          {evaluation?.disclaimer || error?.message || 'Đánh giá AI đang tạm gián đoạn; bài của bạn vẫn được lưu an toàn.'}
        </p>
        {onRetryEvaluation && (
          <button
            type="button"
            onClick={onRetryEvaluation}
            className="mt-4 inline-flex items-center rounded-xl bg-amber-500 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-amber-400"
          >
            Thử lại đánh giá AI
          </button>
        )}
      </div>
    )
  }

  if (!evaluation || evaluation.status !== 'GRADED') {
    return (
      <div className="rounded-3xl border border-amber-500/20 bg-slate-900/60 p-6 text-center text-slate-300 backdrop-blur-xl">
        <p className="text-sm">Chưa có kết quả đánh giá cho phiên bản này.</p>
        {onRetryEvaluation && (
          <button
            type="button"
            onClick={onRetryEvaluation}
            className="mt-4 inline-flex items-center rounded-xl bg-amber-500/20 px-4 py-2 text-sm font-semibold text-amber-300 transition hover:bg-amber-500/30"
          >
            Yêu cầu đánh giá AI
          </button>
        )}
      </div>
    )
  }

  const { overallBandEstimate, criteria = {}, strengths = [], issues = [], suggestions = [], priorityImprovements = [] } = evaluation

  const criterionLabels = {
    taskAchievement: 'Task Achievement (Task 1)',
    taskResponse: 'Task Response (Task 2)',
    coherenceCohesion: 'Coherence & Cohesion',
    lexicalResource: 'Lexical Resource',
    grammaticalRangeAccuracy: 'Grammatical Range & Accuracy',
  }

  return (
    <div className="space-y-6">
      {/* Overall Band Card */}
      <div className="relative overflow-hidden rounded-3xl border border-amber-500/30 bg-gradient-to-br from-slate-900/90 via-amber-950/20 to-slate-900/90 p-6 shadow-2xl backdrop-blur-xl">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <span className="text-xs font-semibold uppercase tracking-wider text-amber-400">
              Kết quả đánh giá
            </span>
            <h2 className="font-serif text-3xl font-bold text-slate-100">
              Band ước lượng bởi AI: <span className="text-amber-300">{overallBandEstimate?.toFixed(1) ?? 'N/A'}</span>
            </h2>
            <p className="mt-1 text-xs text-amber-200/70 italic">
              Band ước lượng bởi AI — Không phải điểm thi IELTS chính thức.
            </p>
          </div>
          {versions.length > 1 && (
            <button
              type="button"
              onClick={() => {
                setShowComparison(!showComparison)
                if (!showComparison && onCompareVersions) {
                  onCompareVersions(versions[1].versionNumber, versions[0].versionNumber)
                }
              }}
              className="rounded-xl border border-amber-500/30 bg-amber-500/10 px-4 py-2 text-xs font-semibold text-amber-200 transition hover:bg-amber-500/20"
            >
              {showComparison ? 'Ẩn so sánh' : 'So sánh các phiên bản'}
            </button>
          )}
        </div>
      </div>

      {/* Comparison View if open */}
      {showComparison && comparisonData && (
        <div className="rounded-3xl border border-amber-500/30 bg-slate-900/80 p-6 backdrop-blur-xl">
          <h3 className="font-serif text-xl font-bold text-amber-200 mb-4">
            So sánh phiên bản v{comparisonData.baseVersionNumber} và v{comparisonData.targetVersionNumber}
          </h3>
          {comparisonData.evaluationsAvailable ? (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
              <div className="rounded-xl bg-emerald-950/30 border border-emerald-500/20 p-4">
                <h4 className="text-xs font-semibold uppercase tracking-wider text-emerald-400 mb-2">Vấn đề đã khắc phục</h4>
                {comparisonData.resolvedIssues?.length > 0 ? (
                  <ul className="list-disc list-inside text-xs text-slate-300 space-y-1">
                    {comparisonData.resolvedIssues.map((issue, idx) => (
                      <li key={idx}>{issue}</li>
                    ))}
                  </ul>
                ) : (
                  <p className="text-xs text-slate-500">Không có</p>
                )}
              </div>
              <div className="rounded-xl bg-amber-950/30 border border-amber-500/20 p-4">
                <h4 className="text-xs font-semibold uppercase tracking-wider text-amber-400 mb-2">Vấn đề còn lặp lại</h4>
                {comparisonData.repeatedIssues?.length > 0 ? (
                  <ul className="list-disc list-inside text-xs text-slate-300 space-y-1">
                    {comparisonData.repeatedIssues.map((issue, idx) => (
                      <li key={idx}>{issue}</li>
                    ))}
                  </ul>
                ) : (
                  <p className="text-xs text-slate-500">Không có</p>
                )}
              </div>
              <div className="rounded-xl bg-sky-950/30 border border-sky-500/20 p-4">
                <h4 className="text-xs font-semibold uppercase tracking-wider text-sky-400 mb-2">Vấn đề mới</h4>
                {comparisonData.newIssues?.length > 0 ? (
                  <ul className="list-disc list-inside text-xs text-slate-300 space-y-1">
                    {comparisonData.newIssues.map((issue, idx) => (
                      <li key={idx}>{issue}</li>
                    ))}
                  </ul>
                ) : (
                  <p className="text-xs text-slate-500">Không có</p>
                )}
              </div>
            </div>
          ) : (
            <p className="text-xs text-slate-400 italic mb-4">Chưa đủ dữ liệu đánh giá cả 2 phiên bản để so sánh chi tiết điểm số.</p>
          )}
        </div>
      )}

      {/* Criteria Breakdown */}
      <div className="rounded-3xl border border-slate-800 bg-slate-900/60 p-6 backdrop-blur-xl">
        <h3 className="font-serif text-xl font-bold text-slate-100 mb-4">
          Chi tiết 4 tiêu chí chấm điểm
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {Object.entries(criteria).map(([key, desc]) => (
            <div key={key} className="rounded-2xl border border-slate-800 bg-slate-800/40 p-4">
              <h4 className="text-sm font-semibold text-amber-300">
                {criterionLabels[key] || key}
              </h4>
              <p className="mt-2 text-xs leading-relaxed text-slate-300">{desc}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Feedback Highlights */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Strengths */}
        <div className="rounded-3xl border border-emerald-500/20 bg-emerald-950/10 p-6 backdrop-blur-xl">
          <h3 className="font-serif text-lg font-bold text-emerald-400 mb-3">Điểm mạnh</h3>
          <ul className="space-y-2 text-xs text-slate-300 list-disc list-inside">
            {strengths.map((str, idx) => (
              <li key={idx}>{str}</li>
            ))}
          </ul>
        </div>

        {/* Priority Improvements & Issues */}
        <div className="rounded-3xl border border-amber-500/20 bg-amber-950/10 p-6 backdrop-blur-xl">
          <h3 className="font-serif text-lg font-bold text-amber-400 mb-3">Ưu tiên cải thiện</h3>
          <ul className="space-y-2 text-xs text-slate-300 list-disc list-inside">
            {(priorityImprovements.length > 0 ? priorityImprovements : issues).map((item, idx) => (
              <li key={idx}>{item}</li>
            ))}
          </ul>
        </div>
      </div>

      {/* Version History */}
      <WritingVersionHistory
        versions={versions}
        activeVersionId={activeVersion?.id}
        onSelectVersion={onSelectVersion}
        onCompare={onCompareVersions}
      />
    </div>
  )
}
