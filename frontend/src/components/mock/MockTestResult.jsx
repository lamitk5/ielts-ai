import React from 'react'
import Button from '../common/Button'
import GlassCard from '../common/GlassCard'

const SKILL_NAMES = {
  LISTENING: 'Listening (Nghe)',
  READING: 'Reading (Đọc)',
  WRITING: 'Writing (Viết)',
  SPEAKING: 'Speaking (Nói)',
}

export default function MockTestResult({
  result,
  onRetake,
  onNavigateCatalog,
  onReviewSection,
}) {
  if (!result) {
    return (
      <div className="p-8 text-center text-slate-500">
        Không có dữ liệu kết quả thi thử.
      </div>
    )
  }

  const isFullyGraded = result.resultStatus === 'READY' && result.estimatedOverallBand != null
  const isPartial = result.resultStatus === 'PARTIALLY_AVAILABLE' || (result.resultStatus === 'READY' && result.estimatedOverallBand == null)
  const isFailed = result.resultStatus === 'FAILED'

  return (
    <div className="max-w-5xl mx-auto space-y-8 py-6 px-4">
      {/* Header Banner */}
      <div className="text-center space-y-2">
        <span className="inline-block px-3 py-1 rounded-full text-xs font-semibold tracking-wider uppercase bg-gold-500/10 text-gold-600 dark:text-gold-300 border border-gold-500/20">
          Kết quả thi thử mô phỏng
        </span>
        <h1 className="font-serif text-3xl md:text-4xl font-bold text-navy-900 dark:text-gold-100">
          Báo Cáo Tổng Hợp Mock Test
        </h1>
        <p className="text-sm text-slate-600 dark:text-slate-400 max-w-2xl mx-auto">
          Mã bài thi: <span className="font-mono text-xs">{result.mockTestId}</span> • Tổng thời gian làm bài:{' '}
          <span className="font-medium">{Math.floor(result.totalDurationSeconds / 60)} phút</span>
        </p>
      </div>

      {/* Overall Band Card or Partial Notice */}
      <GlassCard className="p-6 md:p-8 text-center space-y-4 border-gold-500/30 bg-gradient-to-b from-white/90 to-amber-50/30 dark:from-navy-900/90 dark:to-navy-950/90 shadow-xl">
        {isFullyGraded ? (
          <div className="space-y-3">
            <p className="text-sm font-medium uppercase tracking-wider text-slate-500 dark:text-slate-400">
              {result.estimatedBandLabel || 'Band ước lượng tổng thể'}
            </p>
            <div className="text-6xl md:text-7xl font-serif font-black text-transparent bg-clip-text bg-gradient-to-r from-amber-600 via-gold-500 to-amber-700 dark:from-gold-300 dark:via-gold-400 dark:to-amber-200">
              {result.estimatedOverallBand.toFixed(1)}
            </div>
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-md bg-amber-500/10 border border-amber-500/30 text-xs text-amber-700 dark:text-amber-300">
              <svg className="w-4 h-4 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
              </svg>
              <span>{result.aiDisclaimer || 'Band ước lượng bởi AI — Không phải điểm thi IELTS chính thức.'}</span>
            </div>
          </div>
        ) : isPartial ? (
          <div className="space-y-3 py-2">
            <div className="w-12 h-12 mx-auto rounded-full bg-amber-500/10 text-amber-500 flex items-center justify-center">
              <svg className="w-6 h-6 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
            </div>
            <h2 className="font-serif text-xl font-bold text-navy-900 dark:text-gold-100">
              Kết quả một phần đang được chấm
            </h2>
            <p className="text-sm text-slate-600 dark:text-slate-300 max-w-lg mx-auto">
              Các phần thi trắc nghiệm (Reading / Listening) đã có kết quả tức thì. Các kỹ năng Viết (AI Evaluation) và Nói (Human / Audio Review) đang được xử lý hoặc chờ đánh giá.
            </p>
            <p className="text-xs text-amber-600 dark:text-amber-400 font-medium">
              * Điểm tổng thể (Overall Band) sẽ chỉ hiển thị khi đầy đủ 4 kỹ năng đã được chấm.
            </p>
          </div>
        ) : (
          <div className="space-y-2 py-2 text-red-600 dark:text-red-400">
            <h2 className="font-serif text-xl font-bold">Không thể tải báo cáo hoàn chỉnh</h2>
            <p className="text-sm">Đã có lỗi xảy ra trong quá trình tổng hợp kết quả bài thi thử.</p>
          </div>
        )}
      </GlassCard>

      {/* Section Breakdown Grid */}
      <div className="space-y-4">
        <h2 className="font-serif text-2xl font-bold text-navy-900 dark:text-gold-100">
          Chi tiết từng phần thi
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {result.sectionResults?.map((section, idx) => {
            const skillUpper = (section.skill || '').toUpperCase()
            const name = SKILL_NAMES[skillUpper] || section.skill
            const lr = section.learnerResult
            const score = lr?.score
            const total = lr?.total
            const band = lr?.estimatedBand ?? lr?.aiEvaluation?.estimatedBand ?? lr?.humanReview?.overallBand

            return (
              <GlassCard key={idx} className="p-5 flex flex-col justify-between space-y-4">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="w-5 h-5 rounded-full bg-slate-100 dark:bg-slate-800 text-xs font-mono flex items-center justify-center text-slate-600 dark:text-slate-400">
                        {idx + 1}
                      </span>
                      <h3 className="font-serif font-bold text-base md:text-lg text-navy-900 dark:text-slate-100">
                        {name}
                      </h3>
                    </div>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                      Mã bài: <span className="font-mono">{section.practiceId}</span>
                    </p>
                  </div>

                  {band != null ? (
                    <div className="text-right">
                      <span className="text-2xl font-serif font-black text-amber-600 dark:text-gold-300">
                        {Number(band).toFixed(1)}
                      </span>
                      <span className="block text-[10px] text-slate-400 uppercase tracking-wider">Band</span>
                    </div>
                  ) : (
                    <span className="px-2 py-1 rounded text-xs font-medium bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
                      {section.sectionStatus === 'COMPLETED' ? 'Đang chấm' : 'Chưa hoàn thành'}
                    </span>
                  )}
                </div>

                {/* Specific skill details */}
                <div className="text-xs text-slate-600 dark:text-slate-300 space-y-1.5 pt-2 border-t border-slate-100 dark:border-slate-800">
                  {score != null && total != null && (
                    <div className="flex justify-between">
                      <span>Số câu đúng:</span>
                      <span className="font-semibold">{score} / {total}</span>
                    </div>
                  )}

                  {lr?.aiEvaluation && (
                    <div className="space-y-1">
                      <div className="flex justify-between text-emerald-600 dark:text-emerald-400">
                        <span>Đánh giá AI:</span>
                        <span className="font-semibold">Hoàn thành</span>
                      </div>
                      {lr.aiEvaluation.strengths?.length > 0 && (
                        <p className="text-[11px] text-slate-500 dark:text-slate-400 line-clamp-1">
                          Ưu điểm: {lr.aiEvaluation.strengths[0]}
                        </p>
                      )}
                    </div>
                  )}

                  {lr?.humanReview && (
                    <div className="flex justify-between text-indigo-600 dark:text-indigo-400">
                      <span>Đánh giá giám khảo:</span>
                      <span className="font-semibold">{lr.humanReview.status}</span>
                    </div>
                  )}

                  {lr?.speaking && (
                    <div className="flex justify-between text-slate-500">
                      <span>Ghi âm & Bản ghi:</span>
                      <span>{lr.speaking.audioAvailable ? 'Đã lưu audio' : 'Không có audio'}</span>
                    </div>
                  )}

                  {section.statusMessage && (
                    <p className="text-amber-600 dark:text-amber-400 italic">
                      {section.statusMessage}
                    </p>
                  )}
                </div>

                {/* Action button */}
                {section.submissionId && onReviewSection && (
                  <div className="pt-2">
                    <Button
                      size="sm"
                      variant="ghost"
                      className="w-full text-xs"
                      onClick={() => onReviewSection(section.submissionId, section.skill)}
                    >
                      Xem chi tiết bài làm →
                    </Button>
                  </div>
                )}
              </GlassCard>
            )
          })}
        </div>
      </div>

      {/* Action Footer */}
      <div className="flex flex-wrap items-center justify-center gap-4 pt-4 border-t border-slate-200 dark:border-slate-800">
        {onRetake && (
          <Button variant="primary" size="md" onClick={onRetake}>
            Làm lại bài thi thử mới
          </Button>
        )}
        {onNavigateCatalog && (
          <Button variant="secondary" size="md" onClick={onNavigateCatalog}>
            Quay về danh mục luyện tập
          </Button>
        )}
      </div>
    </div>
  )
}
